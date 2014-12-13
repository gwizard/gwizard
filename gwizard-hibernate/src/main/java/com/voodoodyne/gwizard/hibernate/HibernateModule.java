package com.voodoodyne.gwizard.hibernate;

import com.google.inject.AbstractModule;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.jpa.JpaPersistModule;
import com.google.inject.persist.jpa.WizardBridgeModule;
import com.google.inject.util.Modules;
import javax.inject.Inject;

/**
 */
public class HibernateModule extends AbstractModule {

	static class Initializer {
		@Inject
		public Initializer(PersistService service) {
			service.start();
		}
	}

	@Override
	protected void configure() {
		install(Modules.override(new JpaPersistModule("punit")).with(new WizardBridgeModule()));

		bind(Initializer.class).asEagerSingleton();
	}
}
