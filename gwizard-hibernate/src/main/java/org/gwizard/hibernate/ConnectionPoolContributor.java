package org.gwizard.hibernate;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.engine.jdbc.connections.internal.DataSourceConnectionProvider;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.hikaricp.internal.HikariCPConnectionProvider;
import org.hibernate.service.spi.ServiceContributor;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import static org.hibernate.cfg.JdbcSettings.CONNECTION_PROVIDER;
import static org.hibernate.cfg.JdbcSettings.DATASOURCE;
import static org.hibernate.cfg.JdbcSettings.PASS;
import static org.hibernate.cfg.JdbcSettings.USER;

/**
 * Applies the GWizard fallback only after Hibernate has merged all configuration sources.
 * Must be public for Hibernate's {@link java.util.ServiceLoader} discovery.
 */
public final class ConnectionPoolContributor implements ServiceContributor {
	public static final String POOL_CONFIG = "gwizard.connectionPool";
	public static final String APPLICATION_DATA_SOURCE = "gwizard.applicationDataSource";

	@Override
	public void contribute(final StandardServiceRegistryBuilder builder) {
		if (builder.getSettings().containsKey(POOL_CONFIG))
			builder.addInitiator(new PoolInitiator());
	}

	private static class PoolInitiator extends ConnectionProviderInitiator {
		@Override
		public ConnectionProvider initiateService(final Map<String, Object> settings, final ServiceRegistryImplementor registry) {
			if (settings.get(APPLICATION_DATA_SOURCE) instanceof DataSource dataSource) {
				// A supplied pool already owns its credentials. Do not invoke getConnection(user, password).
				settings.remove(USER);
				settings.remove(PASS);
				final DataSourceConnectionProvider provider = new DataSourceConnectionProvider();
				provider.setDataSource(dataSource);
				return provider; // Hibernate releases this reference on stop, without closing the datasource.
			}

			if (settings.get(CONNECTION_PROVIDER) != null || settings.get(DATASOURCE) != null)
				return super.initiateService(settings, registry);

			// Installing hibernate-hikaricp registers a provider globally. Explicit pool properties
			// must still win over Hibernate's automatic "single registered provider" selection.
			for (final String pool : new String[] {"c3p0", "hikari", "agroal"}) {
				if (settings.keySet().stream().anyMatch(key -> key.startsWith("hibernate." + pool + "."))) {
					final Map<String, Object> explicit = new HashMap<>(settings);
					explicit.put(CONNECTION_PROVIDER, pool);
					return super.initiateService(explicit, registry);
				}
			}

			// Preserve a provider that Hibernate would have auto-selected before our Hikari
			// dependency added another registered implementation to the classpath.
			final var registered = registry.requireService(StrategySelector.class)
					.getRegisteredStrategyImplementors(ConnectionProvider.class).stream()
					.filter(type -> type != HikariCPConnectionProvider.class).toList();
			if (registered.size() == 1) {
				final Map<String, Object> explicit = new HashMap<>(settings);
				explicit.put(CONNECTION_PROVIDER, registered.get(0));
				return super.initiateService(explicit, registry);
			}

			final ConnectionProvider selected = super.initiateService(settings, registry);
			if (selected != null && (selected.getClass() == DriverManagerConnectionProvider.class
					|| selected.getClass() == HikariCPConnectionProvider.class))
				return new DefaultHikariConnectionProvider((ConnectionPoolConfig)settings.get(POOL_CONFIG));
			return selected; // Preserve custom registered providers and multi-tenant connection management.
		}
	}
}
