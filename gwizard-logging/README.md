# GWizard Logging

Sets up Logback logging, routing all the "other" logging mechanisms into logback.
There are a couple ways to manage the configuration.

## Maven

```xml
	<dependency>
		<groupId>com.voodoodyne.gwizard</groupId>
		<artifactId>gwizard-logging</artifactId>
		<version>${gwizard.version}</version>
	</dependency>
```

This pulls in all relevant dependencies.

## Usage: Configured using logback.xml

[A self-contained example](src/test/java/com/voodoodyne/gwizard/logging/example/LoggingModuleExample.java)

If you prefer the [traditional Logback configuration boostrap process](http://logback.qos.ch/manual/configuration.html),
just register the `LoggingModule`:

```java
Injector injector = Guice.createInjector(new LoggingModule());
```

## Usage: Configured from your master config file

It's nice to configure logging in the same place you configure everything else, especially if you have separate
config files for different environments (dev vs staging vs production). However, this means that logging
remains unconfigured (well, it's configured as per the Logback default bootstrap) until after Guice has started and
your configuration file has been processed! This could be an issue, especially if you only monitor logs with an
aggregation service. Imagine you introduce a syntax error into your config file; you'll never see an error at your
aggregator!

Nevertheless, "one single config file" is too useful. To minimize the risk:

1. Register the `LoggingModule` first so it starts up as early as possible.
2. Consider putting some fallback configuration in `logback.xml` for the few microseconds before logging is configured.

At the moment there is only one way to put logging configuration in your master config file:

### XML in your YAML

[A self-contained example](src/test/java/com/voodoodyne/gwizard/logging/example/LoggingModuleExample2.java)

You can also find a complete example in the [GWizard example app](https://github.com/stickfigure/gwizard-example).

Logback's configuration code understands XML, not YAML. So for now, the easiest way to maintain log configuration
is to store a big chunk of XML in your YAML config. Because of YAML's text blocks, this is surprisingly elegant:

```yaml
# this is your example.yml
logging:
  xml: |
    <configuration>
        <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d %5p %40.40c:%4L - %m%n</pattern>
            </encoder>
        </appender>

        <logger name="com.example.app" level="trace"/>

        <root level="info">
            <appender-ref ref="console"/>
        </root>
    </configuration>
```

```java
@Slf4j
public class Main {
	@Data
	public static class MyConfig {
		LoggingConfig logging = new LoggingConfig();
	}

	public static class MyModule extends AbstractModule {
		@Override
		protected void configure() {}

		@Provides
		public LoggingConfig loggingConfig(MyConfig cfg) {
			return cfg.getLogging();
		}
	}

	public static void main(String[] args) throws Exception {
		final Injector injector = Guice.createInjector(
			new LoggingModule(),
			new MyModule(),
			new ConfigModule(new File(args[0], MyConfig.class);

		log.info("Hello!");
	}
}

```

If GWizard finds some content in the `logging.xml` property, it will use this to configure Logback. Otherwise
it leaves the default Logback bootstrap configuration alone.

### YAML in your YAML, yo

It is tempting to want a full YAML definition language for logback configuration. One possibility is that we
could provide some "canned" fixed configuration bindings and translate them to logback config like Dropwizard.
This produces a pretty result but has some
[major shortcomings](https://groups.google.com/forum/#!topic/dropwizard-user/q46TA56CmZk)
as soon as you want to do something new.

It might be better to to provide a 1-to-1 mapping between YAML and XML, so if you create the right YAML
structure you will get the right XML. Conceivably something like this:

```yaml
# this would be your example.yml - note that this is not currently supported
logging:
  appender:
  	name@: console
  	class@: ch.qos.logback.core.ConsoleAppender
  	encoder:
  	  pattern: "%d %5p %40.40c:%4L - %m%n"
	logger:
      name@: com.example.app
      level@: trace

    root:
      level@: info
      appender-ref:
      	ref@: console
```

It's not clear that this is a significant improvement, especially with the necessary mental arithmetic to translate
into XML. If you'd like to see a different solution to this problem,
open up a discussion on the [google group](https://groups.google.com/forum/#!forum/gwizard).

## Contract

* The `LoggingModule` does its hijack of java.util.logging when you *construct* the module itself.
* On app start, looks at a bound `LoggingConfig`; if there is xml present, configures Logback accordingly.
  * The default just-in-time bound `LoggingConfig` does nothing (has no xml).