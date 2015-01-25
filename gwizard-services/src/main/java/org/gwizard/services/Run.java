package org.gwizard.services;

import com.google.common.util.concurrent.ServiceManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;
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
	 * Amount of time to wait for services to finish when a stop happens. Default is 5s.
	 */
	@Getter @Setter
	private int stopTimeoutSeconds = 5;

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
	 * stopTimeoutSeconds wait interval.
	 */
	public void stop() {
		try {
			log.debug("Shutting down services...");
			// Give the services no more than 5 seconds to stop
			serviceManager.get().stopAsync().awaitStopped(stopTimeoutSeconds, TimeUnit.SECONDS);
		} catch (TimeoutException ex) {
			log.error("Timeout waiting for service shutdown", ex);
		}
	}
}
