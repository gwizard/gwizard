package com.google.inject.persist.jpa;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.persist.PersistModule;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.UnitOfWork;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.aopalliance.intercept.MethodInterceptor;
import org.gwizard.hibernate.DatabaseConfig;

import java.util.Map;
import java.util.Properties;

import static org.hibernate.cfg.JdbcSettings.JAKARTA_JDBC_DRIVER;
import static org.hibernate.cfg.JdbcSettings.JAKARTA_JDBC_PASSWORD;
import static org.hibernate.cfg.JdbcSettings.JAKARTA_JDBC_URL;
import static org.hibernate.cfg.JdbcSettings.JAKARTA_JDBC_USER;

/**
 * Reuses Guice Persist's persistence service and request scope, with GWizard configuration and
 * transaction retries. This lives in the Guice package to access its package-private service and
 * {@code @Jpa} qualifier. JpaPersistModule constructs its transaction interceptor directly, so
 * we provide the persistence bindings here and let PersistModule install our interceptor.
 */
public class WizardBridgeModule extends PersistModule {
	private final String persistenceUnitName;
	private MethodInterceptor transactionInterceptor;

	public WizardBridgeModule(final String persistenceUnitName) {
		if (persistenceUnitName == null || persistenceUnitName.isEmpty())
			throw new IllegalArgumentException("JPA unit name must not be empty");
		this.persistenceUnitName = persistenceUnitName;
	}

	@Override
	protected void configurePersistence() {
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

		if (cfg.getProperties() != null)
			props.putAll(cfg.getProperties());

		return props;
	}
}
