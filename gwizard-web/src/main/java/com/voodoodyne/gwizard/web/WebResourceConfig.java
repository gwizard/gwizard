package com.voodoodyne.gwizard.web;

import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import javax.inject.Inject;

/**
 * The jersey/jaxrs resource config. We also give the user a hook to register packages, resources, etc.
 */
public class WebResourceConfig extends ResourceConfig {

	@Inject
	public WebResourceConfig(ObjectMapperContextResolver objectMapperContextResolver, ConfigureJersey configureJersey) {
		property(CommonProperties.METAINF_SERVICES_LOOKUP_DISABLE, true);
		property(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true);
		property(CommonProperties.JSON_PROCESSING_FEATURE_DISABLE, true);
		property(CommonProperties.MOXY_JSON_FEATURE_DISABLE, true);

//		property(ServerProperties.TRACING, "ALL");

		register(objectMapperContextResolver);
		register(JacksonFeature.class);

		configureJersey.configure(this);
	}
}
