# GWizard Metrics

Backend-neutral [Micrometer](https://micrometer.io/) integration for Guice:

* Integration with an application-provided singleton `MeterRegistry`.
* Guice interception of Micrometer's `@Timed` annotation.
* JVM memory, buffers, GC, threads, class-loading, and CPU meters.
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
