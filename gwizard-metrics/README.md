# GWizard Metrics

Incorporates Metrics and Metrics-Guice into GWizard.

See https://github.com/dropwizard/metrics and https://github.com/palominolabs/metrics-guice

So far, this just adds Metrics' JMXReporter to the application. It also
adds metrics-guice's AOP scanning for @Timed, @Metered, etc annotations.

## Maven

```xml
	<dependency>
		<groupId>com.voodoodyne.gwizard</groupId>
		<artifactId>gwizard-metrics</artifactId>
		<version>${gwizard.version}</version>
	</dependency>
```

## Usage

