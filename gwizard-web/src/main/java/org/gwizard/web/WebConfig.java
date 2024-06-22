package org.gwizard.web;

/**
 * Standard configuration options for a GWizard app. Conforms to record format.
 */
public interface WebConfig {
	/** */
	default int getPort() { return 8080; }
}