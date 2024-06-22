package org.gwizard.logging.example;

import ch.qos.logback.classic.Level;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import org.gwizard.logging.LoggingConfig;
import org.gwizard.logging.LoggingConfigProperties;
import org.gwizard.logging.LoggingModule;

import jakarta.inject.Singleton;

import java.util.Map;

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
			return new LoggingConfigProperties(null, Map.of("org.gwizard.logging.example", Level.ERROR));
		}
	}

	public static void main(String[] args) throws Exception {
		final Injector injector = Guice.createInjector(new LoggingModule(), new MyModule());

		log.error("Now you see it");
		log.warn("Now you don't");
	}
}
