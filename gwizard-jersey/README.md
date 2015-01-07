# GWizard Jersey

I'll tell you up front that you should not use this module. You should use the `gwizard-rest` module (which is
built on RESTEasy) instead - it provides a perfectly good JAX-RS implementation with solid Guice support.
This module is for the poor bastards who absolutely *must* use Jersey for one reason or another.

The reason you should not use this module is because Jersey v2 and Guice do not play nice together. The courageous
souls at Squarespace managed to create an adaptor, but it works by using reflection to punch values into private
static final member data in Jersey. This module uses that adaptor; it works for the current version of Jersey,
but it may not work for future versions of Jersey.

Now that you have been warned, here's how to use it.

## Maven

```xml
	<dependency>
		<groupId>com.voodoodyne.gwizard</groupId>
		<artifactId>gwizard-jersey</artifactId>
		<version>${gwizard.version}</version>
	</dependency>
```

This module depends on and installs `WebModule`; you don't need to explicitly register it with the Injector
or specify the gwizard-web maven dependency. You also don't need an explicit maven dependency on gwizard-web.

## Usage

[A self-contained example](src/test/java/com/voodoodyne/gwizard/jersey/example/JerseyModuleExample.java)

```java
public class JerseyModuleExample {
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
			// All resources must be bound so they will be picked up by guice and jersey.
			// You can't just use jersey's package auto-scanning abilities.
			bind(HelloResource.class);
		}
	}

	public static void main(String[] args) throws Exception {
		Guice.createInjector(new MyModule(), new JerseyModule()).getInstance(WebServer.class).startJoin();
	}
}
```

This module just adds some very useful behavior to the `WebModule`. There's a ton of information online about
how to use JAX-RS ([RESTEasy's documentation](http://resteasy.jboss.org/docs.html) is better than
[Jersey's](https://jersey.java.net/documentation/latest/user-guide.html)) so
we'll just assume you know how to make a Resource class. The key to keep in mind is that you "advertise"
resource classes (and providers) to Jersey by binding them in Guice. Jersey's package autoscanning will
not help you.

### Classpath Scanning

Wouldn't it be cool to use Reflections to scan for these JAXRS classes so you don't have to bind them yourself?
Yeah, seems like a good idea for a feature. Easy to add. Request it.

## Contract

* Adds extra behavior to the `WebServer` provided by the GWizard `WebModule`.
* Any JAX-RS-annotated classes which have been bound in Guice are processed.
  * `@Path`-annotated classes become REST endpoints.
  * `@Provider`-annotated classes become JAX-RS providers. This is the JAXRS annotation, not the injection interface!
* See the `WebModule` contract for web server configuration information, or how to start the server.