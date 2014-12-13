package com.voodoodyne.gwizard.logging;

import com.google.inject.AbstractModule;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 */
public class LoggingModule extends AbstractModule {

	public LoggingModule() {
		hijackJDKLogging();
	}

	@Override
	protected void configure() {
	}

	private void hijackJDKLogging() {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();

//		final Logger root = (Logger)LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
//		root.getLoggerContext().reset();
//
//		final LevelChangePropagator propagator = new LevelChangePropagator();
//		propagator.setContext(root.getLoggerContext());
//		propagator.setResetJUL(true);
//
//		root.getLoggerContext().addListener(propagator);
	}

}
