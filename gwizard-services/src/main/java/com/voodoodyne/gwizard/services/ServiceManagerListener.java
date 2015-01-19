package com.voodoodyne.gwizard.services;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import lombok.extern.slf4j.Slf4j;

/**
 * Listener for Service Manager lifecycle
 *
 * <p/>If any services fail, the application will be shutdown. ServicesModule
 * has bound a shutdown hook which will stop other services on shutdown.
 */
@Slf4j
class ServiceManagerListener extends ServiceManager.Listener {
	@Override
	public void failure(Service service) {
		log.error("service failure: {}", service.toString(), service.failureCause());
		System.exit(1);
	}

	@Override
	public void stopped() {
		log.debug("all services stopped");
	}

	@Override
	public void healthy() {
		log.debug("all services healthy");
	}
}
