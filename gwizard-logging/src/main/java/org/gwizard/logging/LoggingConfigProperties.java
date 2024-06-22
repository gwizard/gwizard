package org.gwizard.logging;

import ch.qos.logback.classic.Level;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Optional implementation of LoggingConfig, suitable for reading from yaml.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoggingConfigProperties implements LoggingConfig {
	/** Optional raw xml content for logback config that will replace any preexisting configuration */
	private String xml;

	/** Override any logging levels specified in either the xml or in the logback bootstrap */
	private Map<String, Level> loggers = new LinkedHashMap<>();

	/** Sometimes convenient */
	public LoggingConfigProperties(final String xml) {
		this.xml = xml;
	}
}
