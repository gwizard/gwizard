package org.gwizard.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.dropwizard.util.Duration;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Optional implementation of WebConfig properties.
 */
@Data
@NoArgsConstructor
public class WebConfigProperties implements WebConfig {
	/** */
	private int port = 8080;
	private Duration shutdownTimeout = Duration.seconds(30);

	/** Retains the convenient port-only constructor. */
	public WebConfigProperties(final int port) {
		this.port = port;
	}

	/** YAML uses setters to preserve defaults for omitted properties. */
	@JsonCreator(mode = JsonCreator.Mode.DISABLED)
	public WebConfigProperties(final int port, final Duration shutdownTimeout) {
		this.port = port;
		this.shutdownTimeout = shutdownTimeout;
	}
}
