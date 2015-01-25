package org.gwizard.web;

import com.google.inject.servlet.ServletModule;
import org.gwizard.services.ServicesModule;
import lombok.EqualsAndHashCode;

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
