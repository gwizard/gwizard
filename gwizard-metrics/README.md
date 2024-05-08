# GWizard Metrics

Incorporates Metrics and Metrics-Guice into GWizard.

See https://github.com/dropwizard/metrics and https://github.com/palominolabs/metrics-guice

So far, this just adds Metrics' `JmxReporter` to the application. It also
adds metrics-guice's AOP scanning for @Timed, @Metered, etc annotations.

## Maven

Install the gwizard bom, then:

```xml
    <dependency>
        <groupId>org.gwizard</groupId>
        <artifactId>gwizard-metrics</artifactId>
    </dependency>
```

## Usage

[A self-contained example](src/test/java/org/gwizard/metrics/example/MetricsModuleExample.java)