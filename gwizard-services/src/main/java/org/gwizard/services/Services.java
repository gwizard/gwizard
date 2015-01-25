package org.gwizard.services;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import lombok.extern.slf4j.Slf4j;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 */
@Singleton
@Slf4j
public class Services {

	/** */
	private final List<Service> services = new ArrayList<>();

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
	public void add(Service service) {
		checkNotStarted();
		services.add(service);
	}

	/**
	 * Add a listener that will apply to every service. Must be called before start.
	 */
	public void add(Service.Listener listener) {
		checkNotStarted();
		serviceListeners.add(listener);
	}

	/**
	 * Add a listener to the servicemanager. Must be called before start.
	 */
	public void add(ServiceManager.Listener listener) {
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
		assert !done;
		done = true;

		ServiceManager serviceManager = new ServiceManager(services);

		serviceManager.addListener(new ServiceManagerListener(), MoreExecutors.directExecutor());

		for (ServiceManager.Listener listener : serviceManagerListeners) {
			serviceManager.addListener(listener, MoreExecutors.directExecutor());
		}

		for (Service service : services) {
			service.addListener(new LoggingServiceListener(service), MoreExecutors.directExecutor());

			for (Service.Listener listener : serviceListeners) {
				service.addListener(listener, MoreExecutors.directExecutor());
			}
		}

		return serviceManager;
	}
}
