package com.voodoodyne.gwizard.web;

import com.google.inject.servlet.ServletModule;
import com.voodoodyne.gwizard.services.ServicesModule;

/**
 */
public class WebModule extends ServletModule {
	@Override
	protected void configureServlets() {
		install(new ServicesModule());

		bind(WebServerService.class).asEagerSingleton();
	}
}
