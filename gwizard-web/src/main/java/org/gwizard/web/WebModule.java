package org.gwizard.web;

import com.google.inject.servlet.ServletModule;
import lombok.EqualsAndHashCode;
import org.gwizard.services.ServicesModule;

/**
 */
@EqualsAndHashCode(callSuper=false, of={})	// makes installation of this module idempotent
public class WebModule extends ServletModule {
	@Override
	protected void configureServlets() {
		install(new ServicesModule());

		bind(WebServerService.class).asEagerSingleton();
	}
}
