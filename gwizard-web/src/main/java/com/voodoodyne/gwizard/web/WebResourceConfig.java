package com.voodoodyne.gwizard.web;

import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import javax.inject.Inject;

/**
 * Our basic jersey/jaxrs resource config. If you want to alter behavior, subclass it,
 * add extra logic to the constructor, and bind WebResourceConfig.class to the subclass:
 *
 * <code>bind(WebResourceConfig.class).to(YourSubclass.class);</code>
 */
public class WebResourceConfig extends ResourceConfig {

	@Inject
	public WebResourceConfig(ObjectMapperContextResolver objectMapperContextResolver) {
		property(CommonProperties.METAINF_SERVICES_LOOKUP_DISABLE, true);
		property(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true);
		property(CommonProperties.JSON_PROCESSING_FEATURE_DISABLE, true);
		property(CommonProperties.MOXY_JSON_FEATURE_DISABLE, true);

//		property(ServerProperties.TRACING, "ALL");

		register(objectMapperContextResolver);
		register(JacksonFeature.class);
	}
}
