package com.google.inject.persist.jpa;

import com.google.common.collect.Maps;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.persist.PersistModule;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.UnitOfWork;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.gwizard.hibernate.ConnectionPoolContributor;
import org.gwizard.hibernate.DatabaseConfig;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import static org.hibernate.cfg.JdbcSettings.DATASOURCE;
import static org.hibernate.cfg.JdbcSettings.JAKARTA_JDBC_DRIVER;
import static org.hibernate.cfg.JdbcSettings.JAKARTA_JDBC_PASSWORD;
import static org.hibernate.cfg.JdbcSettings.JAKARTA_JDBC_URL;
import static org.hibernate.cfg.JdbcSettings.JAKARTA_JDBC_USER;
import static org.hibernate.cfg.JdbcSettings.JAKARTA_JTA_DATASOURCE;
import static org.hibernate.cfg.JdbcSettings.JAKARTA_NON_JTA_DATASOURCE;
import static org.hibernate.cfg.JdbcSettings.JPA_JTA_DATASOURCE;
import static org.hibernate.cfg.JdbcSettings.JPA_NON_JTA_DATASOURCE;

/**
 * Reuses Guice Persist's persistence service and request scope, with GWizard configuration and
 * transaction retries. This lives in the Guice package to access its package-private service and
 * {@code @Jpa} qualifier. JpaPersistModule constructs its transaction interceptor directly, so
 * we provide the persistence bindings here and let PersistModule install our interceptor.
 */
@Slf4j
public class WizardBridgeModule extends PersistModule {
	@SuppressWarnings("deprecation") // Legacy datasource keys may still appear in application configuration.
	private static final Set<String> DATA_SOURCE_SETTINGS = Set.of(
			DATASOURCE, JAKARTA_JTA_DATASOURCE, JAKARTA_NON_JTA_DATASOURCE,
			JPA_JTA_DATASOURCE, JPA_NON_JTA_DATASOURCE);

	private final String persistenceUnitName;
	private final Key<? extends DataSource> dataSourceKey;
	private Provider<? extends DataSource> dataSourceProvider;
	private MethodInterceptor transactionInterceptor;

	public WizardBridgeModule(final String persistenceUnitName) {
		this(persistenceUnitName, null);
	}

	public WizardBridgeModule(final String persistenceUnitName, final Key<? extends DataSource> dataSourceKey) {
		if (persistenceUnitName == null || persistenceUnitName.isEmpty())
			throw new IllegalArgumentException("JPA unit name must not be empty");
		this.persistenceUnitName = persistenceUnitName;
		this.dataSourceKey = dataSourceKey;
	}

	@Override
	protected void configurePersistence() {
		if (dataSourceKey != null)
			dataSourceProvider = getProvider(dataSourceKey);
		bindConstant().annotatedWith(Jpa.class).to(persistenceUnitName);
		bind(JpaPersistOptions.class).annotatedWith(Jpa.class).toInstance(JpaPersistOptions.builder().build());
		bind(JpaPersistService.class).in(Singleton.class);
		bind(PersistService.class).to(JpaPersistService.class);
		bind(UnitOfWork.class).to(JpaPersistService.class);
		bind(EntityManager.class).toProvider(JpaPersistService.class);
		bind(EntityManagerFactory.class).toProvider(JpaPersistService.EntityManagerFactoryProvider.class);

		bind(RetryingTransactionInterceptor.class).in(Singleton.class);
		final var provider = getProvider(RetryingTransactionInterceptor.class);
		transactionInterceptor = invocation -> provider.get().invoke(invocation);
		bind(Initializer.class).asEagerSingleton();
	}

	private static class Initializer {
		@Inject
		Initializer(final RetryingTransactionInterceptor transactions, final PersistService service) {
			// Constructing the interceptor validates retry settings before opening the connection pool.
			service.start();
		}

		@com.google.inject.Inject(optional = true)
		void bindPoolMetrics(final MeterRegistry registry, final EntityManagerFactory factory) {
			try {
				final ConnectionProvider provider = factory.unwrap(SessionFactoryImplementor.class)
						.getServiceRegistry().getService(ConnectionProvider.class);
				if (provider != null && provider.isUnwrappableAs(DataSource.class)
						&& provider.unwrap(DataSource.class) instanceof HikariDataSource pool
						&& pool.getMetricRegistry() == null && pool.getMetricsTrackerFactory() == null)
					pool.setMetricRegistry(registry);
			} catch (RuntimeException failure) {
				log.warn("Unable to bind connection pool metrics", failure);
			}
		}
	}

	@Override
	protected MethodInterceptor getTransactionInterceptor() {
		return transactionInterceptor;
	}

	/**
	 * Generate properties out of the database config. The DatabaseConfig must be provided by a user's configuration.
	 */
	@Provides
	@Jpa
	public Map<?, ?> properties(final DatabaseConfig cfg) {
		Properties props = new Properties();

		if (cfg.getDriverClass() != null)
			props.setProperty(JAKARTA_JDBC_DRIVER, cfg.getDriverClass());

		if (cfg.getUrl() != null)
			props.setProperty(JAKARTA_JDBC_URL, cfg.getUrl());

		if (cfg.getUser() != null)
			props.setProperty(JAKARTA_JDBC_USER, cfg.getUser());

		if (cfg.getPassword() != null)
			props.setProperty(JAKARTA_JDBC_PASSWORD, cfg.getPassword());

		if (cfg.getProperties() != null) {
			// An application-supplied datasource takes precedence over configured datasource settings.
			final Map<String, String> cfgProperties = (dataSourceProvider == null)
				? cfg.getProperties()
				: Maps.filterKeys(cfg.getProperties(), key -> !DATA_SOURCE_SETTINGS.contains(key));

			props.putAll(cfgProperties);
		}

		// Selection happens after Hibernate merges these values with persistence.xml.
		props.put(ConnectionPoolContributor.POOL_CONFIG, Objects.requireNonNull(cfg.getPool(), "pool"));
		if (dataSourceProvider != null) {
			final DataSource dataSource = Objects.requireNonNull(dataSourceProvider.get(), "dataSource");
			props.put(JAKARTA_NON_JTA_DATASOURCE, dataSource);
			props.put(ConnectionPoolContributor.APPLICATION_DATA_SOURCE, dataSource);
		}

		return props;
	}
}
