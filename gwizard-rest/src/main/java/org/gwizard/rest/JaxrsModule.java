package org.gwizard.rest;

import com.google.inject.servlet.RequestScoped;
import com.google.inject.servlet.ServletModule;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import javax.inject.Provider;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

/**
 * <p>We need to make the JAXRS classes available. Resteasy provides a RequestScopeModule
 * but that conflicts with Guice's servlet modules (and uses its own @RequestScoped),
 * so we have to duplicate the relevant parts here. I have no idea what the Resteasy
 * team is thinking, but that's not very Guicy.</p>
 *
 * <p>This is automatically installed by the RestModule.</p>
 */
@EqualsAndHashCode(callSuper=false, of={})	// makes installation of this module idempotent
public class JaxrsModule extends ServletModule
{
	@Override
	protected void configureServlets() {
		bind(Request.class).toProvider(new ResteasyContextProvider<>(Request.class)).in(RequestScoped.class);
		bind(HttpHeaders.class).toProvider(new ResteasyContextProvider<>(HttpHeaders.class)).in(RequestScoped.class);
		bind(UriInfo.class).toProvider(new ResteasyContextProvider<>(UriInfo.class)).in(RequestScoped.class);
		bind(SecurityContext.class).toProvider(new ResteasyContextProvider<>(SecurityContext.class)).in(RequestScoped.class);
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
