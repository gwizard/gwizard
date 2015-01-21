package com.voodoodyne.gwizard.services;

import com.google.common.util.concurrent.ServiceManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A prettier interface to the Guava ServiceManager, with simple start() and stop() methods. You can
 * use the ServiceManager as well if you prefer it.
 */
@Singleton
@Slf4j
public class Run {

	/** */
	private final ServiceManager serviceManager;

	/**
	 * Amount of time to wait for services to finish when a stop happens. Default is 5s.
	 */
	@Getter @Setter
	private int stopTimeoutSeconds = 5;

	/** */
	@Inject
	public Run(ServiceManager serviceManager) {
		this.serviceManager = serviceManager;
	}

	/**
	 * Start all services. This is shorthand for {@code serviceManager.startAsync().awaitHealthy();}
	 */
	public void start() {
		serviceManager.startAsync().awaitHealthy();
	}

	/**
	 * Stop all services. This is shorthand for {@code serviceManager.stopAsync().awaitStopped();} with the
	 * stopTimeoutSeconds wait interval.
	 */
	public void stop() {
		try {
			log.debug("Shutting down services...");
			// Give the services no more than 5 seconds to stop
			serviceManager.stopAsync().awaitStopped(stopTimeoutSeconds, TimeUnit.SECONDS);
		} catch (TimeoutException ex) {
			log.error("Timeout waiting for service shutdown", ex);
		}
	}
}
