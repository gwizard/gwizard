package org.gwizard.logging;

import ch.qos.logback.classic.Level;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>Logging configuration is drawn from three sources, in three steps:</p>
 * <ol>
 *     <li>Logging configuraton starts with the standard Logback boostrap process (ie, logback.xml).</li>
 *     <li>If the 'xml' attribute has any content here, the configuration is replaced wholesale.</li>
 *     <li>Any 'loggers' mapped here have their levels overriden appropriately.</li>
 * </ol>
 *
 * <p>The optional 'xml' property should be the raw XML of the logback config file. It will
 * be fed directly to logback. This actually works pretty well in a YAML file because of the
 * nifty block text syntax.</p>
 *
 * <p>If the xml value is null or empty, we leave the standard Logback boostrap config alone.</p>
 */
public interface LoggingConfig {
	/** Optional raw xml content for logback config that will replace any preexisting configuration */
	default String getXml() { return null; }

	/** Override any logging levels specified in either the xml or in the logback bootstrap */
	default Map<String, Level> getLoggers() { return new LinkedHashMap<>(); }
}
