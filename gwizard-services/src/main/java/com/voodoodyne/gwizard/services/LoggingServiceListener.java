package com.voodoodyne.gwizard.services;

import com.google.common.util.concurrent.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
class LoggingServiceListener extends Service.Listener {

	/** */
	private final Logger serviceLogger;

	public LoggingServiceListener(Service service) {
		this.serviceLogger = LoggerFactory.getLogger(service.getClass());
	}

	@Override
	public void failed(Service.State from, Throwable failure) {
		serviceLogger.warn("failed {}", from.name(), failure);
	}

	@Override
	public void terminated(Service.State from) {
		serviceLogger.debug("terminated (from: {})", from);
	}

	@Override
	public void stopping(Service.State from) {
		serviceLogger.debug("stopping (from: {})", from);
	}

	@Override
	public void running() {
		serviceLogger.debug("running");
	}

	@Override
	public void starting() {
		serviceLogger.debug("starting");
	}
}
