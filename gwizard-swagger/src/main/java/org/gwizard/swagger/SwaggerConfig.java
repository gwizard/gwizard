package org.gwizard.swagger;

import lombok.Data;
import org.assertj.core.util.Lists;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * This is the root configuration object for the API specification.
 * All field names in the specification are case sensitive.
 */
@Data
public final class SwaggerConfig {

	/*
	 * The swagger spcification can be faound here: http://swagger.io/specification/
	 * These fields follow the specification.
	 */
	
	/**
	 * Specifies the Swagger Specification version being used. It can be used by the Swagger UI and other clients to 
	 * interpret the API listing. The value MUST be "2.0".
	 */
	@NotNull 
	private String swagger = "2.0";
	
	/**
	 * The host (name or ip) serving the API. This MUST be the host only and does not include the scheme nor sub-paths. 
	 * It MAY include a port. If the host is not included, the host serving the documentation is to be used (including 
	 * the port). The host does not support path templating.
	 */
	private String host = "localhost";
	
	/**
	 * The base path on which the API is served, which is relative to the host. If it is not included, the API is served 
	 * directly under the host. The value MUST start with a leading slash (/). The basePath does not support path 
	 * templating.
	 */
	private String basePath;
	
	/**
	 * The resource packages that should be scanned
	 */
	@NotNull 
	private List<String> resourcePackages = Lists.newArrayList();

	/**
	 * Pretty print the api specification. Normally you'd have this set to true
	 */
	private boolean prettyPrint = true;

}