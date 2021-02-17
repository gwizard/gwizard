package org.gwizard.services;

import com.google.common.util.concurrent.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
class LoggingServiceListener extends Service.Listener {

	/** */
	private final Logger serviceLogger;

	public LoggingServiceListener(final Service service) {
		this.serviceLogger = LoggerFactory.getLogger(service.getClass());
	}

	@Override
	public void failed(final Service.State from, final Throwable failure) {
		serviceLogger.warn("failed {}", from.name(), failure);
	}

	@Override
	public void terminated(final Service.State from) {
		serviceLogger.debug("terminated (from: {})", from);
	}

	@Override
	public void stopping(final Service.State from) {
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
