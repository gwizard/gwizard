# GWizard Metrics

Backend-neutral [Micrometer](https://micrometer.io/) integration for Guice:

* Integration with an application-provided singleton `MeterRegistry`.
* Guice interception of Micrometer's `@Timed` annotation.
* JVM memory, buffers, GC, threads, class-loading, and CPU meters.
* Automatic HTTP, Jetty thread-pool, Hikari connection-pool, and transaction-retry metrics
  when the corresponding GWizard modules are used with a bound `MeterRegistry`.
* Registry and GC-listener cleanup through Gwizard's service lifecycle.

Applications must explicitly bind `MeterRegistry`. There is no default registry;
Guice fails at startup if the binding is missing. Gwizard does not depend on any
backend-specific registry.

## Maven

Install the gwizard bom, then:

```xml
    <dependency>
        <groupId>org.gwizard</groupId>
        <artifactId>gwizard-metrics</artifactId>
    </dependency>
```

## Usage

Install `MetricsModule` and bind your chosen registry as a singleton, then start
services with `Run.start()` as usual. JVM meters are registered at service startup.
On shutdown, Gwizard closes the application-provided registry.

```java
import io.micrometer.core.annotation.Timed;

public class SearchService {
    @Timed(value = "search.duration", description = "Time spent searching")
    public Results search(SearchRequest request) {
        // ...
    }
}
```

Instances must be created by Guice. As with other Guice interceptors, final classes
and final, private, or static methods cannot be intercepted.

`@Timed` can annotate a method or a class. A method annotation overrides the class
annotation; repeated annotations record separate timers. The default meter name is
`method.timed`, with `class`, `method`, and `exception` tags (`none` on success).
Checked and unchecked failures are timed and propagated unchanged.

Methods returning a `CompletionStage` are timed until it completes, not merely until
the method returns. `@Timed(value = "work.active", longTask = true)` instead tracks
in-progress work; long-task timers require an explicit name. Annotation options for
extra tags, descriptions, percentiles, histograms, and service-level objectives are
passed to Micrometer. Histogram/percentile support depends on the registry.

For other instrumentation, inject `MeterRegistry` and use Micrometer's counters,
gauges, timers, and distribution summaries directly. Avoid high-cardinality tags
such as user IDs or raw URLs. Use the injected registry, not Micrometer's static
global registry, which Gwizard does not configure.

### HTTP and database metrics

`WebModule` (including through `RestModule`) and `HibernateModule` automatically use
an explicitly bound `MeterRegistry`. No additional instrumentation module or endpoint
annotations are needed. Without that binding these modules still work normally and
do not record metrics. `MetricsModule` supplies JVM instrumentation and registry
shutdown; the web and Hibernate modules do not close the registry themselves.

| Meter | Meaning |
| --- | --- |
| `http.server.requests` | Timer of completed HTTP exchanges, tagged by `method`, `status`, `outcome`, and `error`. Its count gives request volume/rate; total time, max, and optional histograms give latency. |
| `http.server.requests.active` | Gauge of in-flight requests, including asynchronous responses. |
| `jetty.threads.jobs` | Gauge of work items waiting for a Jetty thread. These include protocol tasks as well as requests, so this is not an exact queued-request count. |
| `jetty.threads.busy`, `.idle`, `.current` | Jetty thread usage; `.config.min` and `.config.max` expose configured limits. Queue and busy gauges are available with Jetty's standard `QueuedThreadPool`. |
| `hikaricp.connections.acquire` | Timer of connection acquisition, including waits that time out. |
| `hikaricp.connections.usage`, `.creation` | Timers of borrowed connection usage and new physical connection creation. |
| `hikaricp.connections.timeout` | Counter of pool acquisition timeouts. |
| `hikaricp.connections`, `.active`, `.idle`, `.pending`, `.max`, `.min` | Pool size, usage, waiting borrowers, and configured capacity. Hikari gauges use its cached pool statistics. |
| `db.transaction.attempts` | Counter of transaction attempts owned by GWizard, including retries. |
| `db.transaction.retries` | Counter of additional attempts actually started after SQLSTATE 40001. Interrupted backoff does not count as a retry. |
| `db.transaction.serialization.failures` | Counter of attempts that failed with SQLSTATE 40001. |
| `db.transaction.retry.exhausted` | Counter of transactions that reached `maxAttempts` with SQLSTATE 40001. |

HTTP timers span request receipt through response completion, including asynchronous
work and response writes. They cover the root servlet context, additional Jetty
handlers, and 503 rejections while draining. Only requests entering the handler tree
are measured; malformed requests rejected before dispatch are outside this coverage.
The `error` tag indicates failure to complete the exchange; a normal HTTP 500 still
has `status=500` and `outcome=SERVER_ERROR` even if `error=false`.

HTTP methods are limited to Jetty's known methods plus `OTHER`. Raw paths, query
strings, user IDs, SQL, and exception messages are never metric tags. For endpoint
timing, use `@Timed` on resource methods. HTTP meters are removed when the server stops.

Pool meters use Hikari's native Micrometer integration and a `pool` tag. This works
with the default pool, explicitly configured Hibernate Hikari pools, and supplied
Hikari datasources. Instrumentation attaches after the persistence unit starts, so
bootstrap acquisitions are not included. An existing Hikari metric registry or
metrics tracker factory is preserved. Other pool implementations require their own
pool instrumentation. Hikari removes its meters when the pool closes; supplied
datasources remain application-owned and are not closed by GWizard.

Transaction counters have a `persistence.unit` tag. Nested transactional calls share
the outer attempt and do not add counts; manually started transactions are excluded.
Counter recording failures do not replace transaction results or prevent retries.

Timers use Micrometer's default distribution settings. Configure histograms or
percentiles on the registry before binding it, for example:

```java
registry.config().meterFilter(new MeterFilter() {
    @Override
    public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
        if (id.getName().equals("http.server.requests")
                || id.getName().equals("hikaricp.connections.acquire")) {
            return DistributionStatisticConfig.builder()
                    .percentilesHistogram(true).build().merge(config);
        }
        return config;
    }
});
```

Use `io.micrometer.core.instrument.config.MeterFilter` and
`io.micrometer.core.instrument.distribution.DistributionStatisticConfig`.
Meter names above are Micrometer names; exporters translate names and time units.
Request rate is the rate of change of the HTTP timer's count. Rising Jetty queues,
pool pending borrowers/acquisition timeouts, and retry exhaustion identify pressure
that may require application or capacity changes.

### Choosing a backend

Add the appropriate `micrometer-registry-*` dependency to your application; its
version is managed by the Gwizard BOM. Use a normal binding in your application
module (or an equivalent `@Provides @Singleton` method):

```java
install(new MetricsModule());
bind(MeterRegistry.class).toProvider(MyRegistryProvider.class).in(Scopes.SINGLETON);
```

Your provider constructs the registry and configures credentials, publishing
intervals, common tags, and meter filters. A push registry manages its own publishing
thread; a pull registry requires an application-provided scrape endpoint. Gwizard
doesn't add either an endpoint or a sidecar. A `CompositeMeterRegistry` can send the
same measurements to multiple destinations.

Configure common tags and filters before returning the registry from the provider.
The registry belongs to this injector's service lifecycle; don't share it between
independently running injectors.

For tests or applications that only need in-memory metrics, explicitly bind a
`SimpleMeterRegistry` instead. It requires no network calls or credentials:

```java
bind(MeterRegistry.class).to(SimpleMeterRegistry.class).in(Scopes.SINGLETON);
```

[A self-contained example](src/test/java/org/gwizard/metrics/example/MetricsModuleExample.java)
