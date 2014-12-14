package com.voodoodyne.gwizard.web;

import com.google.inject.servlet.GuiceFilter;
import com.voodoodyne.gwizard.web.EventListenerScanner.Visitor;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import javax.inject.Inject;
import java.util.EventListener;

/**
 */
public class WebServer {

	private final WebConfig httpConfig;
	private final EventListenerScanner eventListenerScanner;

	@Inject
	public WebServer(WebConfig httpConfig, EventListenerScanner eventListenerScanner) {
		this.httpConfig = httpConfig;
		this.eventListenerScanner = eventListenerScanner;
	}

	public void start() {
		// Create the server.
		final Server server = new Server(httpConfig.getPort());

		// Create a servlet context and add the jersey servlet.
		final ServletContextHandler sch = new ServletContextHandler(server, "/");

		sch.addFilter(GuiceFilter.class, "/*", null);

		// Must add DefaultServlet for embedded Jetty. Failing to do this will cause 404 errors.
		sch.addServlet(DefaultServlet.class, "/");

		// This will add any registered ServletContextListeners or other misc servlet listeners
		// that have been bound. For example, the GuiceResteasyBootstrapServletContextListener
		// which gets bound by gwizard-rest.
		// Sigh no java8
		eventListenerScanner.accept(new Visitor() {
			@Override
			public void visit(EventListener listener) {
				sch.addEventListener(listener);
			}
		});

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
