# GWizard Services

Put simply, a "service" is some program logic with a managed startup and shutdown lifecycle.
Ordinary services start and stop in parallel. Servers that accept incoming work start after
ordinary services are running, and stop before ordinary services are shut down.

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

Use `services.addServer(this)` for a service that accepts incoming work and depends on the
ordinary services. `WebModule` already registers its web server this way. Registration order
does not matter. Services within each phase still start and stop in parallel. A service in
the ordinary phase must not wait for the web server during its own startup.

The same ordering applies when using `Run` or the injected `ServiceManager` directly. When
servers are registered, the manager tracks one coordinated lifecycle, so its
`servicesByState()` and startup timings describe that lifecycle rather than individual services.
Listeners registered with `Services.add(Service.Listener)` still observe the original services.
Application code should start and stop the manager, rather than individual registered services.

Stopping during startup cancels the remaining startup phases. If a service fails or stops
unexpectedly, the lifecycle drains/stops servers and then stops the remaining dependencies
before reporting failure to the manager.
Service failures still request process exit immediately; with `Run`, the shutdown hook waits
for this ordered cleanup within the overall shutdown budget.

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

`Run` registers a JVM shutdown hook and waits up to **40 seconds** for shutdown by default.
This allows the web server's default 30-second request drain plus time to stop dependencies.
Customize the overall wait with `Run.setStopTimeoutSeconds(int)` before shutdown. If you
increase the web drain timeout, increase this overall budget too; the deployment's process
termination grace period must also accommodate it. A service that never finishes stopping
can exhaust this budget; the shutdown hook logs the timeout and returns.
