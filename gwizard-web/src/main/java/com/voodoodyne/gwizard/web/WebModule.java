package com.voodoodyne.gwizard.web;

import com.google.common.util.concurrent.Service;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.servlet.ServletModule;

/**
 * While this doesn't do any actual work, having a ServletModule included is critical to set up
 * the request scope. Maybe it's useful to have this around, especially if we add web-specific
 * bindings in the future.
 */
public class WebModule extends ServletModule {
	@Override
	protected void configureServlets() {
            Multibinder.newSetBinder(binder(), Service.class)
                    .addBinding().to(WebServerLaunchingService.class);
	}
}
