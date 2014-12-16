package com.voodoodyne.gwizard.logging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>At the moment, we allow only one possible property, 'xml'. That is the raw XML
 * of the logback config file to be fed directly to logback. This is not super elegant
 * but it works pretty well in a YAML file and lets us set aside the question of
 * how to translate between YAML and XML.</p>
 *
 * <p>If the xml value is null or empty, we fall back to the standard logback configuration
 * bootstrap routine.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoggingConfig {
	private String xml;
}
