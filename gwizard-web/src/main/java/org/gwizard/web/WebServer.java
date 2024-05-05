package org.gwizard.web;

import com.google.common.base.Preconditions;
import com.google.inject.servlet.GuiceFilter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;

/**
 * Simple Jetty-based embedded web server which configures itself from a bound WebConfig and serves the
 * GuiceFilter so you can manage web content with Guice ServletModules. Also clever enough to add any
 * EventListener objects found in the injector bindings.
 */
@Singleton
public class WebServer {

	private final WebConfig webConfig;
	private final EventListenerScanner eventListenerScanner;
	private final HandlerScanner handlerScanner;

	private Server server;

	@Inject
	public WebServer(final WebConfig webConfig, final EventListenerScanner eventListenerScanner, final HandlerScanner handlerScanner) {
		this.webConfig = webConfig;
		this.eventListenerScanner = eventListenerScanner;
		this.handlerScanner = handlerScanner;
	}

	/**
	 * Start the web server. Does not block.
	 *
	 * @see Server#start()
	 */
	public void start() throws Exception {
		Preconditions.checkState(server == null, "Server already started");

		// Create the server.
		server = createServer(webConfig);

		final ServletContextHandler sch = createRootServletContextHandler();

		sch.addFilter(GuiceFilter.class, "/*", null);

		// Must add DefaultServlet for embedded Jetty. Failing to do this will cause 404 errors.
		sch.addServlet(DefaultServlet.class, "/");

		// This will add any registered ServletContextListeners or other misc servlet listeners
		// that have been bound.
		eventListenerScanner.accept(sch::addEventListener);

        final ContextHandlerCollection handlers = new ContextHandlerCollection();

		// the sch is currently the server handler, add it to the list
		handlers.addHandler(sch);

		// This will add any registered jetty Handlers that have been bound.
		handlerScanner.accept(handlers::addHandler);

		server.setHandler(sch);

		// Start the server
		server.start();
	}

	/**
	 * Overrideable method to create the root ServletContextHandler. This can be used so that the Server can
	 * have a ServletContextHandler that will be able to handle Sessions for example.
	 * By default we create a bare-bones ServletContextHandler that is not set up to handle Sessions.
	 */
	protected ServletContextHandler createRootServletContextHandler() {
		return new ServletContextHandler("/");
	}

	/**
	 * Overrideable method to create the initial jetty Server. We need to draw a lot more configuration parameters
	 * into WebConfig, but for now this gives users a hook to satisfy their needs. Bind a subclass to WebConfig,
	 * subclass this WebServer, and change behavior to whatever you want.
	 */
	protected Server createServer(final WebConfig webConfig) {
		return new Server(webConfig.getPort());
	}

	/**
	 * signal the web server to stop
	 */
	public void stop() throws Exception {
		Preconditions.checkState(server != null, "Server not started");
		server.stop();
	}
}
