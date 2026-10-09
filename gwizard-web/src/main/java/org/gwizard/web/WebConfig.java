package org.gwizard.web;

import io.dropwizard.util.Duration;

/**
 * Standard configuration options for a GWizard app. Conforms to record format.
 */
public interface WebConfig {
	/** */
	default int getPort() { return 8080; }

	/** Maximum time for active requests to finish during shutdown. Zero disables draining. */
	default Duration getShutdownTimeout() { return Duration.seconds(30); }
}
