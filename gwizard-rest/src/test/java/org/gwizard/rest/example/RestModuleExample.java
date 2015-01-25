package org.gwizard.rest.example;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import org.gwizard.rest.RestModule;
import org.gwizard.services.Run;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * Self-contained example of using the RestModule by itself
 */
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
		Guice.createInjector(new MyModule(), new RestModule()).getInstance(Run.class).start();
	}
}
