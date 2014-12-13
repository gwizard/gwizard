package com.voodoodyne.gwizard.web;

import com.google.inject.Provides;
import com.google.inject.servlet.ServletModule;
import com.squarespace.jersey2.guice.BootstrapModule;
import com.squarespace.jersey2.guice.BootstrapUtils;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.servlet.ServletContainer;
import javax.inject.Singleton;

/**
 * <p>The basic web module, which sets up Jersey to service JAXRS requests. Runtime configuration is determined
 * by the WebConfig; provide an implementation if you wish to override defaults.</p>
 *
 * <p>Static configuration of Jersey can be accomplished by manipulating WebResourceConfig during the provision
 * process. Most of the time, all you need to do is specify packages to search.</p>
 *
 * @see WebResourceConfig
 */
public class WebModule extends ServletModule {
	/** */
	private final String[] packages;

	/**
	 * @param packages is a list of packages to search for JAXRS classes. If you do not specify any packages,
	 *                 you will not have any endpoints unless you provide a WebResourceConfig with some registered
	 *                 classes.
	 */
	public WebModule(String... packages) {
		this.packages = packages;
	}

	@Override
	protected void configureServlets() {

		// The order these operations (including the steps in the linker) are important
		ServiceLocator locator = BootstrapUtils.newServiceLocator();
		install(new BootstrapModule(locator));
		bind(HK2Linker.class).asEagerSingleton();

		serve("/*").with(ServletContainer.class);
	}

	@Provides
	@Singleton
	public ServletContainer servletContainer(WebResourceConfig webApp) {
		webApp.packages(packages);
		return new ServletContainer(webApp);
	}
}
