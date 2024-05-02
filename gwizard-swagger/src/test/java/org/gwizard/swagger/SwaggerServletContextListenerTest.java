/**
 * 
 */
package org.gwizard.swagger;


import com.google.common.collect.Lists;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.models.Swagger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author darren
 *
 */
public class SwaggerServletContextListenerTest {

	@Test
	public void testGetBeanConfig() {

		SwaggerConfig swaggerConfig = new SwaggerConfig();

		swaggerConfig.setHost("localhost:8081");
		swaggerConfig.setBasePath("/");
		swaggerConfig.setResourcePackages(Lists.newArrayList("com.gwizard", "com.belldj"));

		BeanConfig beanConfig = SwaggerServletContextListener.createBeanConfig(swaggerConfig);

		assertThat(beanConfig).isNotNull();
		assertThat(beanConfig.getScan()).isEqualTo(true);
		assertThat(beanConfig.getHost()).isEqualTo("localhost:8081");
		assertThat(beanConfig.getBasePath()).isEqualTo("/");
		assertThat(beanConfig.getResourcePackage()).isEqualTo("com.gwizard,com.belldj");

		Swagger swagger = beanConfig.getSwagger();

		assertThat(swagger).isNotNull();
	}
}
