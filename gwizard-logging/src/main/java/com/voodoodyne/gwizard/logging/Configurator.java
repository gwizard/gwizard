package com.voodoodyne.gwizard.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

/**
 * Takes care of setting up the logging environment; should be bound as eager singleton. The sooner it executes,
 * the better.
 */
@Slf4j
public class Configurator {
	@Inject
	public Configurator(LoggingConfig loggingConfig) {

		if (loggingConfig.getXml() == null || loggingConfig.getXml().trim().isEmpty()) {
			log.debug("No explicit configuration for logging; leaving logback's default discovery process alone");
			return;
		}

		// Via http://logback.qos.ch/xref/chapters/configuration/MyApp3.html

		LoggerContext context = (LoggerContext)LoggerFactory.getILoggerFactory();

		try {
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(context);
			// Call context.reset() to clear any previous configuration, e.g. default
			// configuration. For multi-step configuration, omit calling context.reset().
			context.reset();
			configurator.doConfigure(new ByteArrayInputStream(loggingConfig.getXml().getBytes("utf-8")));
		} catch (UnsupportedEncodingException | JoranException je) {
			throw new RuntimeException(je);
		}
	}
}
