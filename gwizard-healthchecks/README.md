# GWizard Health Checks

Incorporates Dropwizard's HealthChecks into GWizard. This health-check API is
independent of the Micrometer registry used for metrics.

See the [Dropwizard Health Checks documentation](https://metrics.dropwizard.io/4.2.0/manual/healthchecks.html).

## Maven

Install the gwizard bom, then:

```xml
    <dependency>
        <groupId>org.gwizard</groupId>
        <artifactId>gwizard-healthchecks</artifactId>
    </dependency>
```

## Usage

Install `HealthChecksModule` and bind your health checks as eager singletons.
Checks extending `AbstractMetricReportingHealthCheck` also publish Micrometer
gauges; install `MetricsModule` alongside this module and explicitly bind a singleton
`MeterRegistry`.
Publishing is controlled by the application's chosen registry, not by this module.

Gauge names combine `HealthChecksConfig.getMetricsPrefix()` (default
`gwizard.healthChecks`) and the check name. Values are `1` for healthy, `0` for
unhealthy, and `NaN` when the check throws. The optional cache interval throttles
gauge evaluation, but doesn't change explicit or scheduled health checks.

[A self-contained example](src/test/java/org/gwizard/healthchecks/HealthChecksModuleExample.java)
