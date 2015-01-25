package org.gwizard.jersey.example;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import org.gwizard.jersey.JerseyModule;
import org.gwizard.services.Run;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * Self-contained example of using the JerseyModule by itself
 */
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
			// All resources must be bound so they will be picked up by resteasy
			bind(HelloResource.class);
		}
	}

	public static void main(String[] args) throws Exception {
		Guice.createInjector(new MyModule(), new JerseyModule()).getInstance(Run.class).start();
	}
}
