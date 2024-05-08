# GWizard Services

Put simply, a "service" is some program logic that starts up in a separate thread. Because of
this, services are started and stopped in parallel.

GWizard Services dynamically loads Guava Services and starts them with Guava ServiceManager.
Other GWizard modules use this facility to run web servers, metrics servers, etc but you can
also use it directly to implement Services which handle their own startup/shutdown processing and execution
strategies.

See https://github.com/google/guava/wiki/ServiceExplained 

## Maven

Install the gwizard bom, then:

```xml
    <dependency>
        <groupId>org.gwizard</groupId>
        <artifactId>gwizard-services</artifactId>
    </dependency>
```

## Usage

If you are simply using the other GWizard modules that depend on this module, you
can ignore gwizard-services. Those other modules configure ServicesModule.

If you want to explicitly configure services, you can register them like this:

[A self-contained example](src/test/java/org/gwizard/services/example/ServicesModuleExample.java)

Example service:

```java
    @Singleton
    @Slf4j
    public class ExampleService extends AbstractIdleService {
        @Inject
        public ExampleService(Services services) {
            services.add(this);
        }

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

Adding a service to the injected Services object registers it. You can also add
`Service.Listener` and `ServiceManager.Listener` objects.

You can obtain a guava `ServiceManager` directly from the injector and start it,
or you can use the convenient `Run` object:

```java
    Injector injector = Guice.createInjector(
        //... other modules ...
        new ServicesModule()
    );

    injector.getInstance(Run.class).start();
```

`injector.getInstance(Run.class).start();` is effectively the same as `injector.getInstance(ServiceManager.class).startAsync().awaitHealthy();`

To shutdown all services:

```java
    injector.getInstance(Run.class).stop();
```

