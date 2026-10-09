package org.gwizard.web;

import com.google.common.base.Preconditions;
import com.google.inject.servlet.GuiceFilter;
import io.dropwizard.util.Duration;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.ee11.servlet.DefaultServlet;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.GracefulHandler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Simple Jetty-based embedded web server which configures itself from a bound WebConfig and serves the
 * GuiceFilter so you can manage web content with Guice ServletModules. Also clever enough to add any
 * EventListener objects found in the injector bindings.
 */
@Singleton
@Slf4j
public class WebServer {

	private final WebConfig webConfig;
	private final EventListenerScanner eventListenerScanner;
	private final HandlerScanner handlerScanner;

	private Server server;
	private GracefulHandler gracefulHandler;
	private MeterRegistry meterRegistry;

	@com.google.inject.Inject(optional = true)
	void setMeterRegistry(final MeterRegistry registry) {
		this.meterRegistry = registry;
	}

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
		final Duration shutdownTimeout = webConfig.getShutdownTimeout();
		Preconditions.checkArgument(shutdownTimeout != null && shutdownTimeout.getQuantity() >= 0,
				"shutdownTimeout must not be null or negative");
		Preconditions.checkArgument(shutdownTimeout.getQuantity() == 0 || shutdownTimeout.toMilliseconds() >= 1,
				"shutdownTimeout must be zero or at least 1 ms");

		// Create the server.
		server = createServer(webConfig);
		server.setStopTimeout(shutdownTimeout.toMilliseconds());

		final ServletContextHandler sch = createRootServletContextHandler();

		sch.addFilter(GuiceFilter.class, "/*", null);

		// Must add DefaultServlet for embedded Jetty. Failing to do this will cause 404 errors.
		sch.addServlet(DefaultServlet.class, "/");

		// This will add any registered ServletContextListeners or other misc servlet listeners
		// that have been bound.
		eventListenerScanner.accept(sch::addEventListener);

		final ContextHandlerCollection handlers = new ContextHandlerCollection();

		// Include both the root servlet context and any additional contexts in the lifecycle.
		handlers.addHandler(sch);

		// This will add any registered jetty Handlers that have been bound.
		handlerScanner.accept(handlers::addHandler);

		gracefulHandler = new GracefulHandler(handlers);
		server.setHandler(meterRegistry == null ? gracefulHandler : new HttpMetricsHandler(gracefulHandler, meterRegistry));

		// Start the server
		try {
			server.start();
		} catch (Exception | Error failure) {
			// A failed Guava service is not stopped again, so clean up partial Jetty startup here.
			server.setStopTimeout(0);
			try {
				server.stop();
			} catch (Exception | Error cleanupFailure) {
				failure.addSuppressed(cleanupFailure);
			}
			throw failure;
		}
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
		try {
			// Reject new requests, but keep normal connection timeouts while active requests drain.
			// Server.stop() would otherwise shorten every connection's idle timeout to one second.
			if (server.getStopTimeout() > 0)
				gracefulHandler.shutdown().get(server.getStopTimeout(), TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			log.warn("Request drain timed out; closing remaining connections");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw e;
		} finally {
			// Draining is complete (or its budget expired). Close idle connections promptly too.
			server.setStopTimeout(0);
			server.stop();
		}
	}
}
