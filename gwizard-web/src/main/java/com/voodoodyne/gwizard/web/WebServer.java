package com.voodoodyne.gwizard.web;

import com.google.inject.servlet.GuiceFilter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import javax.inject.Inject;

/**
 */
public class WebServer {

	private final WebConfig httpConfig;

	@Inject
	public WebServer(WebConfig httpConfig) {
		this.httpConfig = httpConfig;
	}

	public void start() {
		// Create the server.
		Server server = new Server(httpConfig.getPort());

		// Create a servlet context and add the jersey servlet.
		ServletContextHandler sch = new ServletContextHandler(server, "/");

		sch.addFilter(GuiceFilter.class, "/*", null);

		// Must add DefaultServlet for embedded Jetty. Failing to do this will cause 404 errors.
		sch.addServlet(DefaultServlet.class, "/");

		// Start the server
		try {
			server.start();
			server.join();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
