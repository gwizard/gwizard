package org.gwizard.hibernate;

import com.google.inject.AbstractModule;
import com.google.inject.persist.jpa.WizardBridgeModule;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

/**
 * <p>This module provides a little bit of glue between guice-persist and the rest of gwizard.
 * It configures JPA through a guice-friendly config mechanism and retries serialization failures
 * at the transaction boundary. Just provide a DatabaseConfig object.</p>
 *
 * <p>Unfortunately there is no way around requiring your app to have a META-INF/persistence.xml
 * file in the same jar that has your entity classes. However, this can be a minimal skeleton
 * file which contains only the &lt;provider&gt; definition of org.hibernate.jpa.HibernatePersistenceProvider.
 * See the gwizard-example application for a demonstration.</p>
 */
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper=false, of={})	// makes installation of this module idempotent
public class HibernateModule extends AbstractModule {

	private final String persistenceUnitName;

	/** Assume a default persistence unit name of "gw-persistence-unit" */
	public HibernateModule() {
		this("gw-persistence-unit");
	}

	@Override
	protected void configure() {
		install(new WizardBridgeModule(persistenceUnitName));
		requestStaticInjection(EM.class);
	}
}
