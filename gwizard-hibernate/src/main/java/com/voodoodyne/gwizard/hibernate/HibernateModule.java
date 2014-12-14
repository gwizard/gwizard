package com.voodoodyne.gwizard.hibernate;

import com.google.inject.AbstractModule;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.jpa.JpaPersistModule;
import com.google.inject.persist.jpa.WizardBridgeModule;
import com.google.inject.util.Modules;
import lombok.RequiredArgsConstructor;
import javax.inject.Inject;

/**
 * <p>This module provides a little bit of glue between guice-persist and the rest of gwizard.
 * The main benefit is the ability to configure JPA through a guice-friendly config mechanism.
 * Just provide a DbConfig object.</p>
 *
 * <p>Unfortunately there is no way around requiring your app to have a META-INF/persistence.xml
 * file in the same jar that has your entity classes. However, this can be a minimal skeleton
 * file which contains only the &lt;provider&gt; definition of org.hibernate.jpa.HibernatePersistenceProvider.
 * See the gwizard-example application for a demonstration.</p>
 */
@RequiredArgsConstructor
public class HibernateModule extends AbstractModule {

	private final String persistenceUnitName;

	/** Assume a default persistence unit name of "gw-persistence-unit" */
	public HibernateModule() {
		this("gw-persistence-unit");
	}

	static class Initializer {
		@Inject
		public Initializer(PersistService service) {
			service.start();
		}
	}

	@Override
	protected void configure() {
		install(Modules.override(new JpaPersistModule(persistenceUnitName)).with(new WizardBridgeModule()));

		bind(Initializer.class).asEagerSingleton();

		requestStaticInjection(EM.class);
	}
}
