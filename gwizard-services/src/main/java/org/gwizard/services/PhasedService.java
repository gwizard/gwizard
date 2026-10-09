package org.gwizard.services;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

/** Starts dependencies before servers, and stops servers before dependencies. */
final class PhasedService extends AbstractService {

	private final List<Service> dependencies;
	private final List<Service> servers;
	private final List<Service> all;

	// Serialize lifecycle callbacks without blocking a service thread waiting for another service.
	private final Executor transitions = MoreExecutors.newSequentialExecutor(MoreExecutors.directExecutor());
	private boolean serversStarted;
	private boolean serversStopping;
	private boolean dependenciesStopping;
	private boolean finished;
	private Throwable failure;

	PhasedService(final List<Service> dependencies, final List<Service> servers) {
		this.dependencies = List.copyOf(dependencies);
		this.servers = List.copyOf(servers);
		this.all = Stream.concat(dependencies.stream(), servers.stream()).toList();
		addListener(new Listener() {
			@Override
			public void terminated(final State from) {
				if (from == State.NEW) {
					PhasedService.this.servers.forEach(Service::stopAsync);
					PhasedService.this.dependencies.forEach(Service::stopAsync);
				}
			}
		}, MoreExecutors.directExecutor());
	}

	@Override
	protected void doStart() {
		transitions.execute(() -> {
			for (final Service service : all) {
				service.addListener(new Service.Listener() {
					@Override public void running() { changed(); }
					@Override public void stopping(final State from) { changed(); }
					@Override public void terminated(final State from) { changed(); }
					@Override public void failed(final State from, final Throwable cause) { changed(); }
					private void changed() { transitions.execute(PhasedService.this::advance); }
				}, MoreExecutors.directExecutor());
			}
			start(dependencies);
			advance();
		});
	}

	@Override
	protected void doStop() {
		transitions.execute(this::advance);
	}

	@Override
	protected void doCancelStart() {
		doStop();
	}

	private void start(final List<Service> services) {
		for (final Service service : services) {
			if (state() != State.STARTING || failure != null)
				break;
			try {
				service.startAsync();
			} catch (RuntimeException e) {
				failure = e;
			}
		}
	}

	private void advance() {
		if (finished)
			return;

		for (final Service service : all) {
			if (failure == null && service.state() == State.FAILED)
				failure = service.failureCause();
			if (failure == null && state() != State.STOPPING
					&& (service.state() == State.STOPPING || service.state() == State.TERMINATED))
				failure = new IllegalStateException("Service stopped unexpectedly: " + service);
		}
		if (failure != null)
			stopAsync();

		if (state() == State.STOPPING) {
			if (!serversStopping) {
				serversStopping = true;
				servers.forEach(Service::stopAsync);
			}
			if (stopped(servers) && !dependenciesStopping) {
				dependenciesStopping = true;
				dependencies.forEach(Service::stopAsync);
			}
			if (stopped(all)) {
				// A service can fail synchronously inside stopAsync(), before its callback is processed.
				for (final Service service : all)
					if (failure == null && service.state() == State.FAILED)
						failure = service.failureCause();
				finished = true;
				if (failure == null)
					notifyStopped();
				else
					notifyFailed(failure);
			}
		} else {
			if (!serversStarted && dependencies.stream().allMatch(Service::isRunning)) {
				serversStarted = true;
				start(servers);
				transitions.execute(this::advance);
			}
			if (state() == State.STARTING && all.stream().allMatch(Service::isRunning))
				notifyStarted();
		}
	}

	private static boolean stopped(final List<Service> services) {
		return services.stream().allMatch(service -> service.state() == State.TERMINATED || service.state() == State.FAILED);
	}
}
