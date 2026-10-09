package org.gwizard.services;

import com.google.common.util.concurrent.ServiceManager;
import io.dropwizard.util.Duration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.util.concurrent.TimeoutException;

/**
 * <p>A prettier interface to the Guava ServiceManager, with simple start() and stop() methods. Registers
 * a Runtime shutdown hook to stop all services.</p>
 *
 * <p>You can use the ServiceManager directly instead, but you'll have to register your own shutdown hook.</p>
 */
@Singleton
@Slf4j
public class Run {

	/** */
	private final Provider<ServiceManager> serviceManager;

	/**
	 * Amount of time to wait for services to finish when a stop happens. Default is 40s, allowing
	 * the web server's default 30s request drain plus time to stop dependencies.
	 */
	@Getter
	private Duration stopTimeout = Duration.seconds(40);

	public void setStopTimeout(final Duration stopTimeout) {
		if (stopTimeout == null || stopTimeout.getQuantity() < 0)
			throw new IllegalArgumentException("stopTimeout must not be null or negative");
		this.stopTimeout = stopTimeout;
	}

	/** */
	@Inject
	public Run(Provider<ServiceManager> serviceManager) {
		this.serviceManager = serviceManager;

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				Run.this.stop();
			}
		});
	}

	/**
	 * Start all services. This is shorthand for {@code serviceManager.startAsync().awaitHealthy();}
	 */
	public void start() {
		serviceManager.get().startAsync().awaitHealthy();
	}

	/**
	 * Stop all services. This is shorthand for {@code serviceManager.stopAsync().awaitStopped();} with the
	 * stopTimeout wait interval.
	 */
	public void stop() {
		try {
			log.debug("Shutting down services...");
			final Duration timeout = stopTimeout;
			serviceManager.get().stopAsync().awaitStopped(timeout.getQuantity(), timeout.getUnit());
		} catch (TimeoutException ex) {
			log.error("Timeout waiting for service shutdown", ex);
		}
	}
}
