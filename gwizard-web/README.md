# GWizard Web

Provides a Jetty web server so that your application can start up and serve requests. If you're building
REST services, you probably won't install this module directly - it's installed automatically by
the `RestModule`. If you just want to `serve()` or `filter()` requests from your Guice ServletModules,
this is your huckleberry.

## Maven

```xml
	<dependency>
		<groupId>org.gwizard</groupId>
		<artifactId>gwizard-web</artifactId>
		<version>${gwizard.version}</version>
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
* Provides (but does not create or start) a `WebServer`.
  * Instantiate it and start it at your leisure.
* The `WebServer` looks for any bound `EventListener` objects relevant to servlets such as `ServletContextListener`,
`ServletContextAttributeListener`, etc and calls them as appropriate.
