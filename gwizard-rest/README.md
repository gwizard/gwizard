# GWizard REST

`RestModule` is the meat-and-potatoes. Sets up RESTEasy such that any JAX-RS annotated classes (`@Path`) which
are bound in Guice become REST endpoints.

This module depends on and installs `WebModule`; you don't need to explicitly register it with the Injector
or specify the gwizard-web maven dependency.

## Maven

```xml
	<dependency>
		<groupId>com.voodoodyne.gwizard</groupId>
		<artifactId>gwizard-rest</artifactId>
		<version>${gwizard.version}</version>
	</dependency>
```

## Usage

[A self-contained example](src/test/java/com/voodoodyne/gwizard/rest/RestModuleExample.java)

```java
public class RestModuleExample {
	/** A standard JAX-RS resource class */
	@Path("/hello")
	public static class HelloResource {
		@GET
		public String hello() {
			return "hello, world";
		}
	}

	public static class MyModule extends AbstractModule {
		@Override
		protected void configure() {
			// All resources must be bound so they will be picked up by resteasy
			bind(HelloResource.class);
		}
	}

	public static void main(String[] args) throws Exception {
		Guice.createInjector(new MyModule(), new RestModule()).getInstance(WebServer.class).startJoin();
	}
}
```

This module just adds some very useful behavior to the `WebModule`. There's a ton of information online about
how to use JAX-RS ([RESTEasy's documentation](http://resteasy.jboss.org/docs.html) is better than Jersey's) so
we'll just assume you know how to make a Resource class. The key to keep in mind is that you "advertise"
resource classes (and providers) to RESTEasy by binding them in Guice.

### Classpath Scanning

Wouldn't it be cool to use Reflections to scan for these JAXRS classes so you don't have to bind them yourself?
Yeah, seems like a good idea for a feature. Easy to add. Request it.

## Contract

* Adds extra behavior to the `WebServer` provided by the GWizard `WebModule`.
* Any JAX-RS-annotated classes which have been bound in Guice are processed.
  * `@Path`-annotated classes become REST endpoints.
  * `@Provider`-annotated classes become JAX-RS providers. This is the JAXRS annotation, not the injection interface!
* See the `WebModule` contract for web server configuration information, or how to start the server.