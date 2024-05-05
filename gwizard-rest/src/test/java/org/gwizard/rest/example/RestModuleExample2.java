package org.gwizard.rest.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Provides;
import lombok.Data;
import org.gwizard.rest.RestModule;
import org.gwizard.services.Run;

import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Date;

/**
 * Self-contained example of using the RestModule by itself, but with
 * some extra configuration of the ObjectMapper.
 */
public class RestModuleExample2 {
	@Data
	public static class Thing {
		private final String foo;
		private final Date bar;
	}

	@Path("/things")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public static class ThingsResource {
		@GET
		public Thing thing() {
			return new Thing("fun", new Date());
		}
	}

	public static class MyModule extends AbstractModule {
		@Override
		protected void configure() {
			bind(ThingsResource.class);
		}

		@Provides
		@Singleton
		public ObjectMapper objectMapper() {
			return new ObjectMapper()
					.enable(SerializationFeature.INDENT_OUTPUT)
					.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		}
	}

	public static void main(String[] args) throws Exception {
		Guice.createInjector(new MyModule(), new RestModule()).getInstance(Run.class).start();
	}
}
