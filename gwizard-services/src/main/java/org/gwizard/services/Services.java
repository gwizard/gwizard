package org.gwizard.services;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import lombok.extern.slf4j.Slf4j;

import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 */
@Singleton
@Slf4j
public class Services {

	/** */
	private final List<Service> services = new ArrayList<>();
	private final List<Service> servers = new ArrayList<>();

	/** */
	private final List<Service.Listener> serviceListeners = new ArrayList<>();

	/** */
	private final List<ServiceManager.Listener> serviceManagerListeners = new ArrayList<>();

	/** After we have created a ServiceManager, this flag goes true and prevents new services from being added */
	private boolean done;

	/**
	 * Add a service that will be managed on startup. Typically you create services by @Injecting the
	 * Services object and calling add(this). Must be called before start.
	 */
	public void add(final Service service) {
		checkNotStarted();
		Preconditions.checkArgument(!services.contains(service), "Service already registered: %s", service);
		services.add(service);
	}

	/**
	 * Register a service that accepts incoming work. Servers start after all ordinary services are
	 * running, and must finish draining and stopping before ordinary services are stopped.
	 */
	public void addServer(final Service service) {
		add(service);
		servers.add(service);
	}

	/**
	 * Add a listener that will apply to every service. Must be called before start.
	 */
	public void add(final Service.Listener listener) {
		checkNotStarted();
		serviceListeners.add(listener);
	}

	/**
	 * Add a listener to the servicemanager. Must be called before start.
	 */
	public void add(final ServiceManager.Listener listener) {
		checkNotStarted();
		serviceManagerListeners.add(listener);
	}

	private void checkNotStarted() {
		Preconditions.checkState(!done, "You can't add services after the system has started");
	}

	/**
	 * Create a service manager from the present data. This should be done only once.
	 */
	ServiceManager makeServiceManager() {
		Preconditions.checkState(!done, "ServiceManager already created; it should be a singleton");
		done = true;

		for (final Service service : services)
			Preconditions.checkState(service.state() == Service.State.NEW, "Service already started: %s", service);

		final List<Service> managed = servers.isEmpty() ? services : List.of(new PhasedService(
				services.stream().filter(service -> !servers.contains(service)).toList(), servers));
		final ServiceManager serviceManager = new ServiceManager(managed);

		final ServiceManagerListener lifecycleListener = new ServiceManagerListener();
		serviceManager.addListener(lifecycleListener, MoreExecutors.directExecutor());

		for (final ServiceManager.Listener listener : serviceManagerListeners) {
			serviceManager.addListener(listener, MoreExecutors.directExecutor());
		}

		for (final Service service : services) {
			service.addListener(new LoggingServiceListener(service), MoreExecutors.directExecutor());
			if (!servers.isEmpty()) {
				service.addListener(new Service.Listener() {
					@Override
					public void failed(final Service.State from, final Throwable cause) {
						// Preserve fail-fast exit even if cleanup hangs. Run's shutdown hook bounds
						// the wait for the coordinated lifecycle, which remains stoppable meanwhile.
						lifecycleListener.failure(service);
					}
				}, MoreExecutors.directExecutor());
			}

			for (final Service.Listener listener : serviceListeners) {
				service.addListener(listener, MoreExecutors.directExecutor());
			}
		}

		return serviceManager;
	}
}
