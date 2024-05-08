/*
 */

package org.gwizard.guiceeasy;

import com.google.inject.Injector;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.jboss.resteasy.plugins.server.servlet.FilterDispatcher;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;


/**
 * After you have bound the ResteasyModule, map this filter in your guice servlet module
 * to serve specific paths. Note that all the normal configuration init parameters for
 * the Resteasy {@code FilterDispatcher} apply here, including "resteasy.servlet.mapping.prefix".
 */
@Singleton
public class GuiceeasyFilterDispatcher extends FilterDispatcher {

	private final Injector injector;

	@Inject
	public GuiceeasyFilterDispatcher(final Injector injector) {
		this.injector = injector;
	}

	/**
	 */
	@Override
	public void init(final FilterConfig cfg) throws ServletException {
		super.init(cfg);

		final ResteasyProviderFactory providerFactory = getDispatcher().getProviderFactory();
		final Registry registry = (Registry)getDispatcher().getDefaultContextObjects().get(Registry.class);

		final ModuleProcessor processor = new ModuleProcessor(registry, providerFactory);

		processor.processInjector(injector);
	}
}
