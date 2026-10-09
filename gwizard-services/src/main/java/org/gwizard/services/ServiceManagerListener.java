package org.gwizard.services;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntConsumer;

/**
 * Listener for Service Manager lifecycle
 *
 * <p/>If any services fail, the application will be shutdown. ServicesModule
 * has bound a shutdown hook which will stop other services on shutdown.
 */
@Slf4j
class ServiceManagerListener extends ServiceManager.Listener {
	private final AtomicBoolean exiting = new AtomicBoolean();
	private final IntConsumer exit;

	ServiceManagerListener() {
		this(System::exit);
	}

	ServiceManagerListener(final IntConsumer exit) {
		this.exit = exit;
	}

	@Override
	public void failure(final Service service) {
		if (!exiting.compareAndSet(false, true))
			return;
		log.error("Service failure: {}", service.toString(), service.failureCause());
		// System.exit waits for shutdown hooks. Keep this callback free so the lifecycle can
		// keep making progress while Run's hook drains servers and stops dependencies.
		final Thread thread = new Thread(() -> exit.accept(1), "gwizard-service-failure");
		thread.setDaemon(true);
		thread.start();
	}

	@Override
	public void stopped() {
		log.info("All services stopped");
	}

	@Override
	public void healthy() {
		log.info("All services healthy");
	}
}
