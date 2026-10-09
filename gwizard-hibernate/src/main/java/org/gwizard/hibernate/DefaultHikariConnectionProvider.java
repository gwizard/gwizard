package org.gwizard.hibernate;

import org.hibernate.hikaricp.internal.HikariCPConnectionProvider;

import java.util.HashMap;
import java.util.Map;

import static org.hibernate.cfg.JdbcSettings.POOL_SIZE;

/** Hibernate owns and closes the default pool along with the persistence unit. */
final class DefaultHikariConnectionProvider extends HikariCPConnectionProvider {
	private final ConnectionPoolConfig config;

	DefaultHikariConnectionProvider(final ConnectionPoolConfig config) {
		this.config = config;
	}

	@Override
	public void configure(final Map<String, Object> settings) {
		config.validate();
		final Map<String, Object> defaults = new HashMap<>(settings);
		if (!defaults.containsKey(POOL_SIZE))
			defaults.put("hibernate.hikari.maximumPoolSize", config.getMaxPoolSize());
		defaults.put("hibernate.hikari.connectionTimeout", config.getConnectionTimeoutMillis());
		defaults.put("hibernate.hikari.validationTimeout", Math.max(250, Math.min(5_000, config.getConnectionTimeoutMillis() / 2)));
		super.configure(defaults);
	}
}
