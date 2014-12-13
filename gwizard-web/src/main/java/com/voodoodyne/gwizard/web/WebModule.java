package com.voodoodyne.gwizard.web;

import com.google.inject.Provides;
import com.google.inject.servlet.ServletModule;
import com.squarespace.jersey2.guice.BootstrapModule;
import com.squarespace.jersey2.guice.BootstrapUtils;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.servlet.ServletContainer;
import javax.inject.Singleton;

/**
 */
public class WebModule extends ServletModule {
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
		return new ServletContainer(webApp);
	}
}
