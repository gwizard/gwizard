package org.gwizard.guiceeasy;

import com.google.inject.servlet.RequestScoped;
import com.google.inject.servlet.ServletModule;
import jakarta.inject.Provider;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Variant;
import jakarta.ws.rs.ext.RuntimeDelegate;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

/**
 * This module makes jaxrs/resteasy objects available for injection in your Guice app.
 * You will also want to map the {@code GuiceeasyFilterDispatcher} or
 * {@code GuiceeasyHttpServletDispatcher} to a path so that resteasy will serve requests.
 */
@EqualsAndHashCode(callSuper=false, of={})	// makes installation of this module idempotent
public class GuiceeasyModule extends ServletModule {

	private static class ResponseBuilderProvider implements Provider<Response.ResponseBuilder> {
		@Override
		public ResponseBuilder get() {
			return RuntimeDelegate.getInstance().createResponseBuilder();
		}
	}

	private static class UriBuilderProvider implements Provider<UriBuilder> {
		@Override
		public UriBuilder get() {
			return RuntimeDelegate.getInstance().createUriBuilder();
		}
	}

	private static class VariantListBuilderProvider implements Provider<Variant.VariantListBuilder> {
		@Override
		public Variant.VariantListBuilder get() {
			return RuntimeDelegate.getInstance().createVariantListBuilder();
		}
	}

	@Override
	protected void configureServlets() {
		bind(Request.class).toProvider(new ResteasyContextProvider<>(Request.class)).in(RequestScoped.class);
		bind(HttpHeaders.class).toProvider(new ResteasyContextProvider<>(HttpHeaders.class)).in(RequestScoped.class);
		bind(UriInfo.class).toProvider(new ResteasyContextProvider<>(UriInfo.class)).in(RequestScoped.class);
		bind(SecurityContext.class).toProvider(new ResteasyContextProvider<>(SecurityContext.class)).in(RequestScoped.class);

		bind(Response.ResponseBuilder.class).toProvider(ResponseBuilderProvider.class);
		bind(UriBuilder.class).toProvider(UriBuilderProvider.class);
		bind(Variant.VariantListBuilder.class).toProvider(VariantListBuilderProvider.class);
	}

	@RequiredArgsConstructor
	private static class ResteasyContextProvider<T> implements Provider<T> {

		private final Class<T> instanceClass;

		@Override
		public T get() {
			return ResteasyProviderFactory.getInstance().getContextData(instanceClass);
		}
	}
}
