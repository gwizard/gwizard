package com.voodoodyne.gwizard.web;

import org.glassfish.jersey.server.ResourceConfig;

/**
 * Callback interface that lets applications configure Jersey. Apps need to bind an implementation
 * to this interface.
 */
public interface ConfigureJersey {

	/**
	 * When this is called, set up your package scanning or
	 * @param config
	 */
	void configure(ResourceConfig config);
}
