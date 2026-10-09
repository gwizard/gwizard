package org.gwizard.hibernate;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.persist.jpa.WizardBridgeModule;
import lombok.EqualsAndHashCode;

import javax.sql.DataSource;
import java.util.Objects;

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
@EqualsAndHashCode(callSuper=false, of={"persistenceUnitName", "dataSourceKey"})
public class HibernateModule extends AbstractModule {

	private final String persistenceUnitName;
	private final Key<? extends DataSource> dataSourceKey;

	/** Assume a default persistence unit name of "gw-persistence-unit" */
	public HibernateModule() {
		this("gw-persistence-unit");
	}

	public HibernateModule(final String persistenceUnitName) {
		this(persistenceUnitName, null);
	}

	private HibernateModule(final String persistenceUnitName, final Key<? extends DataSource> dataSourceKey) {
		this.persistenceUnitName = persistenceUnitName;
		this.dataSourceKey = dataSourceKey;
	}

	/** Return a module using this application's datasource binding. The application owns its lifecycle. */
	public HibernateModule withDataSource(final Key<? extends DataSource> key) {
		return new HibernateModule(persistenceUnitName, Objects.requireNonNull(key, "dataSourceKey"));
	}

	/** Convenience overload for an unqualified datasource binding. */
	public HibernateModule withDataSource(final Class<? extends DataSource> type) {
		return withDataSource(Key.get(type));
	}

	@Override
	protected void configure() {
		install(new WizardBridgeModule(persistenceUnitName, dataSourceKey));
		requestStaticInjection(EM.class);
	}
}
