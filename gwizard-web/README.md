# GWizard Web

Provides a Jetty web server so that your application can start up and serve requests. If you're building
REST services, you probably won't install this module directly - it's installed automatically by
the `RestModule`. If you just want to `serve()` or `filter()` requests from your Guice ServletModules,
this is your huckleberry.

## Maven

Install the gwizard bom, then:

```xml
    <dependency>
        <groupId>org.gwizard</groupId>
        <artifactId>gwizard-web</artifactId>
    </dependency>
```

## Usage

[A self-contained example](src/test/java/org/gwizard/web/example/WebModuleExample.java)

```java
public class WebModuleExample {
    @Singleton
    public static class HelloServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.getWriter().println("Hello, World");
        }
    }

    public static class MyModule extends ServletModule {
        @Override
        protected void configureServlets() {
            serve("/hello").with(HelloServlet.class);
        }
    }

    public static void main(String[] args) throws Exception {
        final Injector injector = Guice.createInjector(new MyModule(), new WebModule());
        injector.getInstance(Run.class).start();
    }
```

The default port is 8080. You can control web server behavior by binding an instance of `WebConfig`,
which you probably want to draw out of your master config object (see gwizard-config). See the self-contained
example (or the [gwizard-example application](https://github.com/gwizard/gwizard-example) for a more thorough
example.

## Startup and shutdown

The web server starts accepting connections only after all ordinary services registered with
`Services.add(...)` are running. During shutdown, it rejects new requests with 503 and lets
active requests finish before stopping servlet filters, listeners, and other services. This
also covers asynchronous servlet requests and additional Jetty contexts registered in Guice.
Idle connections are closed once active requests have drained.

`WebConfig.getShutdownTimeoutMillis()` defaults to **30,000 milliseconds**. The corresponding
`WebConfigProperties` property is `shutdownTimeoutMillis`; zero disables the drain wait, and
negative values are rejected. When the deadline expires, remaining connections are closed and
shutdown continues. This is a drain deadline, not a timeout imposed on ordinary requests.

`Run` waits up to 40 seconds for the complete shutdown by default. If you increase the drain
timeout, also increase `Run.setStopTimeoutSeconds(...)` to allow time to stop dependencies.
Configure your deployment's termination grace period to cover the overall shutdown budget.
Calling the injected `ServiceManager` directly preserves the same ordering, but you supply its
wait timeout and shutdown hook yourself.

No readiness URL is reserved by the library. Until ordinary services are ready there is no
HTTP listener; while draining, HTTP probes receive 503 along with other new requests.

`PersistFilter` and existing static persistence access can continue to be used. Filter cleanup
runs after request draining, as part of stopping the servlet context.

## Customizing Jetty

If you look closely you'll note that `WebConfig` is rather thin on configuration options - that's because this
framework is very new. If you need to fine-tune Jetty and you can't wait for us to make a new release, subclass
`WebServer` and override `createServer()`. Then bind it:

```java
// In your module's configure()
bind(WebServer.class).to(YourWebServer.class);
```

Ain't Guice grand?

Don't forget to tell us about all the things we need to add to `WebConfig`!

Note: This module uses the ServicesModule. You do not need to explicitly configure the ServicesModule as well.

## Contract

* `WebModule` draws configuration from a bound `WebConfig`.
* Registers `WebServerService` to start and stop `WebServer` in the server lifecycle phase.
* The `WebServer` looks for any bound `EventListener` objects relevant to servlets such as `ServletContextListener`,
`ServletContextAttributeListener`, etc and calls them as appropriate.
