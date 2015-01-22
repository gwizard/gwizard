package com.voodoodyne.gwizard.jersey;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.servlet.ServletModule;
import com.squarespace.jersey2.guice.BootstrapModule;
import com.squarespace.jersey2.guice.BootstrapUtils;
import com.voodoodyne.gwizard.web.WebModule;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Type;

/**
 */
@Slf4j
@EqualsAndHashCode(of={})	// makes installation of this module idempotent
public class JerseyModule extends ServletModule {

	@Override
	protected void configureServlets() {
		install(new WebModule());

		// The order these operations (including the steps in the linker) are important
		ServiceLocator locator = BootstrapUtils.newServiceLocator();
		install(new BootstrapModule(locator));
		bind(HK2Linker.class).asEagerSingleton();

		serve("/*").with(ServletContainer.class);
	}

	@Provides
	@Singleton
	public ServletContainer servletContainer(JerseyResourceConfig webApp, Injector injector) {
		// The timing of this seems to be fairly critical
		registerEverythingInGuiceWithJersey(webApp, injector);

		return new ServletContainer(webApp);
	}

	/**
	 * Walks through everything relevant registered with Guice and registers that with Jersey. This is
	 * so we don't have to register everything separately with Jersey.
	 */
	private void registerEverythingInGuiceWithJersey(final ResourceConfig resourceConfig, final Injector injector) {

		for (final Binding<?> binding : injector.getBindings().values()) {
			final Type type = binding.getKey().getTypeLiteral().getType();
			if (type instanceof Class) {
				final Class<?> beanClass = (Class)type;

				if (beanClass.isAnnotationPresent(Path.class) || beanClass.isAnnotationPresent(Provider.class)) {
					log.debug("Registering {}", beanClass);
					resourceConfig.register(beanClass);
				}
			}
		}
	}
}
