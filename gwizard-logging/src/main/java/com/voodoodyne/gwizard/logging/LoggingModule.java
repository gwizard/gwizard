package com.voodoodyne.gwizard.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.jul.LevelChangePropagator;
import com.google.inject.AbstractModule;
import lombok.EqualsAndHashCode;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 */
@EqualsAndHashCode(callSuper=false, of={})	// makes installation of this module idempotent
public class LoggingModule extends AbstractModule {

	public LoggingModule() {
		hijackJDKLogging();
	}

	@Override
	protected void configure() {
		bind(Configurator.class).asEagerSingleton();
	}

	private void hijackJDKLogging() {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();

		final Logger root = (Logger)LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

		final LevelChangePropagator propagator = new LevelChangePropagator();
		propagator.setContext(root.getLoggerContext());
		propagator.setResetJUL(true);

		root.getLoggerContext().addListener(propagator);
	}

}
