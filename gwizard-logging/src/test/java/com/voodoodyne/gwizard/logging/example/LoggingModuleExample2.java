package com.voodoodyne.gwizard.logging.example;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.voodoodyne.gwizard.logging.LoggingConfig;
import com.voodoodyne.gwizard.logging.LoggingModule;
import lombok.extern.slf4j.Slf4j;
import javax.inject.Singleton;

/**
 * Demonstrates more explicit management of logging configuration. Normally you would extract the LoggingConfig
 * from your master Config object (created using the ConfigModule) but we want this to be a standalone example.
 */
@Slf4j
public class LoggingModuleExample2 {
	public static class MyModule extends AbstractModule {
		private static final String LOGBACK_CONFIG_XML =
				"<configuration>\n" +
				"    <appender name=\"console\" class=\"ch.qos.logback.core.ConsoleAppender\">\n" +
				"        <encoder>\n" +
				"            <pattern>%d %5p %40.40c:%4L - %m%n</pattern>\n" +
				"        </encoder>\n" +
				"    </appender>\n" +
				"\n" +
				"    <root level=\"info\">\n" +
				"        <appender-ref ref=\"console\"/>\n" +
				"    </root>\n" +
				"</configuration>";

		@Override
		protected void configure() {}

		@Provides
		@Singleton
		public LoggingConfig loggingConfig() {
			return new LoggingConfig(LOGBACK_CONFIG_XML);
		}
	}

	public static void main(String[] args) throws Exception {
		final Injector injector = Guice.createInjector(new MyModule(), new LoggingModule());

		log.debug("You won't see this");
		log.info("You will see this");
	}
}
