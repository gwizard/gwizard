package com.google.inject.persist.jpa;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.gwizard.hibernate.DatabaseConfig;

import java.util.Map;
import java.util.Properties;

import static org.hibernate.cfg.AvailableSettings.DRIVER;
import static org.hibernate.cfg.AvailableSettings.PASS;
import static org.hibernate.cfg.AvailableSettings.URL;
import static org.hibernate.cfg.AvailableSettings.USER;

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
	 * Generate properties out of the database config. The DatabaseConfig must be provided by a user's configuration.
	 */
	@Provides
	@Jpa
	public Map<?, ?> properties(final DatabaseConfig cfg) {
		Properties props = new Properties();
		props.setProperty(DRIVER, cfg.getDriverClass());
		props.setProperty(URL, cfg.getUrl());
		props.setProperty(USER, cfg.getUser());
		props.setProperty(PASS, cfg.getPassword());

		props.putAll(cfg.getProperties());

		return props;
	}
}
