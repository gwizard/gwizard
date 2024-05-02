package org.gwizard.swagger;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import io.swagger.config.ScannerFactory;
import io.swagger.jaxrs.config.BeanConfig;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.List;

/**
 * A context listener that scans for APIs and configures Swagger.
 *
 * @author ben.manes@gmail.com (Ben Manes)
 */
@Slf4j
final class SwaggerServletContextListener implements ServletContextListener {

	private final SwaggerConfig swaggerConfig;

	/**
	 * @param swaggerConfig
	 */
	@Inject
	SwaggerServletContextListener(SwaggerConfig swaggerConfig) {
		this.swaggerConfig = swaggerConfig;
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		log.debug("Handling servlet context event. Configuring Swagger...");
		BeanConfig beanConfig = createBeanConfig(swaggerConfig);
		event.getServletContext().setAttribute("reader", beanConfig);
		event.getServletContext().setAttribute("swagger", beanConfig.getSwagger());
		event.getServletContext().setAttribute("scanner", ScannerFactory.getScanner());
	}

	/**
	 * Returns a newly configured {@code BeanConfig} using the provided {@code swaggerConfig}
	 *
	 * @param swaggerConfig The configuration to use
	 * @return a newly configured {@code BeanConfig}
	 */
	static BeanConfig createBeanConfig(SwaggerConfig swaggerConfig) {

		BeanConfig beanConfig = new BeanConfig();

		beanConfig.setHost(swaggerConfig.getHost());
		beanConfig.setBasePath(swaggerConfig.getBasePath());
		beanConfig.setPrettyPrint(swaggerConfig.isPrettyPrint());

		// Must be called last!
		List<String> resourcePackages = swaggerConfig.getResourcePackages();
		beanConfig.setResourcePackage(Joiner.on(",").join(resourcePackages));
		if (resourcePackages.isEmpty()) {
			log.warn("No resource packages configured");
		} else {
			log.debug("Added resource packages {} to swagger bean config", resourcePackages);
		}

		// setScan() actually performs it! (must go last.
		beanConfig.setScan(true);

		return beanConfig;

	}

	@Override
	public void contextDestroyed(ServletContextEvent event) { /* empty */ }

}