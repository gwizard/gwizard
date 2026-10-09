package org.gwizard.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.dropwizard.util.Duration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Optional implementation of WebConfig properties.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(onConstructor_ = @JsonCreator(mode = JsonCreator.Mode.DISABLED)) // YAML uses setters to preserve defaults.
public class WebConfigProperties implements WebConfig {
	/** */
	private int port = 8080;
	private Duration shutdownTimeout = Duration.seconds(30);

	/** Retains the convenient port-only constructor. */
	public WebConfigProperties(final int port) {
		this.port = port;
	}
}
