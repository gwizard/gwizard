package com.voodoodyne.gwizard.logging.example;

import ch.qos.logback.classic.Level;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.voodoodyne.gwizard.logging.LoggingConfig;
import com.voodoodyne.gwizard.logging.LoggingModule;
import lombok.extern.slf4j.Slf4j;
import javax.inject.Singleton;

/**
 * This uses logback's default configuration bootstrap routine, looking for logback.xml etc.
 * However, it lets us override a couple things.
 */
@Slf4j
public class LoggingModuleExample {
	public static class MyModule extends AbstractModule {
		@Override
		protected void configure() {}

		@Provides
		@Singleton
		public LoggingConfig loggingConfig() {
			// Normally you would pull this from a larger config object
			LoggingConfig cfg = new LoggingConfig();
			cfg.getLoggers().put("com.voodoodyne.gwizard.logging.example", Level.ERROR);
			return cfg;
		}
	}

	public static void main(String[] args) throws Exception {
		final Injector injector = Guice.createInjector(new LoggingModule(), new MyModule());

		log.error("Now you see it");
		log.warn("Now you don't");
	}
}
