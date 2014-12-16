package com.voodoodyne.gwizard.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Takes care of setting up the logging environment; should be bound as eager singleton. The sooner it executes,
 * the better.
 */
@Slf4j
public class Configurator {
	@Inject
	public Configurator(LoggingConfig loggingConfig) {

		if (loggingConfig.getXml() == null || loggingConfig.getXml().trim().isEmpty()) {
			log.debug("No explicit configuration for logging; leaving logback's default discovery process intact");
		} else {
			// Via http://logback.qos.ch/xref/chapters/configuration/MyApp3.html
			LoggerContext context = (LoggerContext)LoggerFactory.getILoggerFactory();

			try {
				JoranConfigurator configurator = new JoranConfigurator();
				configurator.setContext(context);
				context.reset();    // start from scratch
				configurator.doConfigure(new ByteArrayInputStream(loggingConfig.getXml().getBytes("utf-8")));
			} catch (UnsupportedEncodingException | JoranException je) {
				throw new RuntimeException(je);
			}
		}

		// Include any level overrides from the config
		for (Map.Entry<String, Level> entry : loggingConfig.getLoggers().entrySet()) {
			((Logger)LoggerFactory.getLogger(entry.getKey())).setLevel(entry.getValue());
		}
	}
}
