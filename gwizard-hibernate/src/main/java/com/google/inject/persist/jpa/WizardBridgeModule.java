package com.google.inject.persist.jpa;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.voodoodyne.gwizard.hibernate.DbConfig;
import java.util.Properties;

/**
 * This allows us to pull database config out of Guice instead of hardcoding it in the
 * JpaPersistModule module as designed. Because we need access to the package-protected @Jpa
 * annotation, we need to put this module in the google package.
 *
 * Note that this module needs to override the JpaPersistModule.
 */
public class WizardBridgeModule extends AbstractModule {
	@Override
	protected void configure() {
	}

	/**
	 * Generate properties out of the db config. The DbConfig must be provided by a user's configuration.
	 */
	@Provides
	@Jpa
	public Properties properties(DbConfig dbConfig) {
		Properties props = new Properties();
		props.setProperty("hibernate.connection.driver_class", dbConfig.getDriverClass());
		props.setProperty("hibernate.connection.url", dbConfig.getUrl());
		props.setProperty("hibernate.connection.username", dbConfig.getUser());
		props.setProperty("hibernate.connection.password", dbConfig.getPassword());

		props.putAll(dbConfig.getProperties());

		return props;
	}
}
