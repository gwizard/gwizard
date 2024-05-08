/*
 */

package org.gwizard.guiceeasy;

import com.google.inject.Injector;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResteasyProviderFactory;


/**
 * After you have bound the ResteasyModule, map this servlet in your guice servlet module
 * to serve specific paths. Note that all the normal configuration init parameters for
 * the Resteasy {@code HttpServletDispatcher} apply here, including "resteasy.servlet.mapping.prefix".
 */
@Singleton
public class GuiceeasyHttpServletDispatcher extends HttpServletDispatcher {

	private final Injector injector;

	@Inject
	public GuiceeasyHttpServletDispatcher(final Injector injector) {
		this.injector = injector;
	}

	/**
	 */
	@Override
	public void init(final ServletConfig cfg) throws ServletException {
		super.init(cfg);

		final ResteasyProviderFactory providerFactory = getDispatcher().getProviderFactory();
		final Registry registry = (Registry)getDispatcher().getDefaultContextObjects().get(Registry.class);

		final ModuleProcessor processor = new ModuleProcessor(registry, providerFactory);

		processor.processInjector(injector);
	}
}
