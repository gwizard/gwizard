package org.gwizard.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Optional implementation of WebConfig properties.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebConfigProperties implements WebConfig {
	/** */
	private int port = 8080;
	private long shutdownTimeoutMillis = 30_000;

	/** Retains the convenient port-only constructor. */
	public WebConfigProperties(final int port) {
		this.port = port;
	}
}
