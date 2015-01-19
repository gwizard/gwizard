# GWizard Services

Dynamically loads Guava Services and starts them with Guava Service Manager.

This allows each module to implement Services which handle their own application
startup/shutdown processing.

See https://code.google.com/p/guava-libraries/wiki/ServiceExplained 

## Maven

```xml
	<dependency>
		<groupId>com.voodoodyne.gwizard</groupId>
		<artifactId>gwizard-services</artifactId>
		<version>${gwizard.version}</version>
	</dependency>
```

## Usage

This module binds a Provider for Guava's Service Manager. Your code 
implements Guava Services, and binds them into a Guice multibound set.

Example service:

```java
    @Singleton
    @Slf4j
    public class ExampleService extends AbstractIdleService {
        @Override
        protected void startUp() throws Exception {
            log.info("This is where my service does something at startup");
        }

        @Override
        protected void shutDown() throws Exception {
            log.info("This is where my service does something at shutdown");
        }
    }
```

Here's how that would be bound

```java
    public static class ExampleModule extends AbstractModule {
        @Override
        protected void configure() {
            Multibinder.newSetBinder(binder(), Service.class)
                    .addBinding().to(ExampleService.class);
        }
    }
```

Here's the application code to retrieve the ServiceManager from the 
injector and wait for services to start at application start.

```java
    Injector injector = Guice.createInjector(
                    //... other modules ...
                    new ServicesModule()
                    );


    // start services
    injector.getInstance(ServiceManager.class).startAsync().awaitHealthy();
```

and, to shutdown the services and exit:

```java
    injector.getInstance(ServiceManager.class).stopAsync().awaitStopped(5, TimeUnit.SECONDS);
    System.exit(0);
```


