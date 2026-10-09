package org.gwizard.services;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhasedServiceTest {

	static class ControlledService extends AbstractService {
		boolean started;
		RuntimeException stopFailure;
		@Override protected void doStart() { started = true; }
		@Override protected void doStop() { if (stopFailure != null) notifyFailed(stopFailure); }
		@Override protected void doCancelStart() { doStop(); }
		void ready() { notifyStarted(); }
		void stopped() { notifyStopped(); }
		void fail(final Throwable failure) { notifyFailed(failure); }
	}

	@Test
	void injectedManagerContractStartsDependenciesFirstAndStopsThemLast() throws Exception {
		final Services registry = new Services();
		final ControlledService first = new ControlledService();
		final ControlledService second = new ControlledService();
		final ControlledService server = new ControlledService();
		registry.addServer(server); // Registration order must not determine lifecycle order.
		registry.add(first);
		registry.add(second);
		final ServiceManager manager = registry.makeServiceManager();

		manager.startAsync();
		assertThat(first.state()).isEqualTo(Service.State.STARTING);
		assertThat(second.state()).isEqualTo(Service.State.STARTING);
		first.ready();
		assertThat(server.state()).isEqualTo(Service.State.NEW);
		second.ready();
		assertThat(server.state()).isEqualTo(Service.State.STARTING);
		assertThat(manager.isHealthy()).isFalse();
		server.ready();
		manager.awaitHealthy(5, TimeUnit.SECONDS);

		manager.stopAsync();
		assertThat(manager.isHealthy()).isFalse();
		assertThat(server.state()).isEqualTo(Service.State.STOPPING);
		assertThat(first.isRunning()).isTrue();
		assertThat(second.isRunning()).isTrue();
		server.stopped();
		assertThat(first.state()).isEqualTo(Service.State.STOPPING);
		assertThat(second.state()).isEqualTo(Service.State.STOPPING);
		first.stopped();
		second.stopped();
		manager.awaitStopped(5, TimeUnit.SECONDS);
	}

	@Test
	void stoppingDuringDependencyStartupNeverStartsTheServer() throws Exception {
		final ControlledService dependency = new ControlledService();
		final ControlledService server = new ControlledService();
		final PhasedService lifecycle = new PhasedService(List.of(dependency), List.of(server));
		lifecycle.startAsync();
		lifecycle.stopAsync();
		assertThat(server.state()).isEqualTo(Service.State.TERMINATED);
		assertThat(server.started).isFalse();
		assertThat(dependency.state()).isEqualTo(Service.State.STOPPING);
		dependency.stopped();
		lifecycle.awaitTerminated(5, TimeUnit.SECONDS);
	}

	@Test
	void stoppingBeforeStartupStopsAllChildren() {
		final ControlledService dependency = new ControlledService();
		final ControlledService server = new ControlledService();
		new PhasedService(List.of(dependency), List.of(server)).stopAsync().awaitTerminated();
		assertThat(dependency.state()).isEqualTo(Service.State.TERMINATED);
		assertThat(server.state()).isEqualTo(Service.State.TERMINATED);
	}

	@Test
	void dependencyStartupFailureCleansUpOtherServicesWithoutStartingServer() {
		final ControlledService failing = new ControlledService();
		final ControlledService other = new ControlledService();
		final ControlledService server = new ControlledService();
		final PhasedService lifecycle = new PhasedService(List.of(failing, other), List.of(server));
		final RuntimeException failure = new RuntimeException("startup failed");
		lifecycle.startAsync();
		other.ready();
		failing.fail(failure);
		assertThat(server.started).isFalse();
		assertThat(other.state()).isEqualTo(Service.State.STOPPING);
		other.stopped();
		assertFailed(lifecycle, failure);
	}

	@Test
	void runtimeFailureDrainsServersBeforeStoppingHealthyDependencies() {
		final ControlledService failing = new ControlledService();
		final ControlledService other = new ControlledService();
		final ControlledService server = new ControlledService();
		final PhasedService lifecycle = new PhasedService(List.of(failing, other), List.of(server));
		lifecycle.startAsync();
		failing.ready();
		other.ready();
		server.ready();
		final RuntimeException failure = new RuntimeException("dependency failed");
		failing.fail(failure);
		assertThat(server.state()).isEqualTo(Service.State.STOPPING);
		assertThat(other.isRunning()).isTrue();
		server.stopped();
		assertThat(other.state()).isEqualTo(Service.State.STOPPING);
		other.stopped();
		assertFailed(lifecycle, failure);
	}

	@Test
	void serverStartupFailureStopsDependencies() {
		final ControlledService dependency = new ControlledService();
		final ControlledService server = new ControlledService();
		final PhasedService lifecycle = new PhasedService(List.of(dependency), List.of(server));
		lifecycle.startAsync();
		dependency.ready();
		final RuntimeException failure = new RuntimeException("bind failed");
		server.fail(failure);
		assertThat(dependency.state()).isEqualTo(Service.State.STOPPING);
		dependency.stopped();
		assertFailed(lifecycle, failure);
	}

	@Test
	void failedServerShutdownStillStopsDependencies() {
		final ControlledService dependency = new ControlledService();
		final ControlledService server = new ControlledService();
		final PhasedService lifecycle = new PhasedService(List.of(dependency), List.of(server));
		lifecycle.startAsync();
		dependency.ready();
		server.ready();
		server.stopFailure = new RuntimeException("stop failed");
		lifecycle.stopAsync();
		assertThat(dependency.state()).isEqualTo(Service.State.STOPPING);
		dependency.stopped();
		assertFailed(lifecycle, server.stopFailure);
	}

	@Test
	void synchronousShutdownFailureIsNotLostWithNoDependencies() {
		final ControlledService server = new ControlledService();
		final PhasedService lifecycle = new PhasedService(List.of(), List.of(server));
		lifecycle.startAsync();
		server.ready();
		server.stopFailure = new RuntimeException("stop failed");
		lifecycle.stopAsync();
		assertFailed(lifecycle, server.stopFailure);
	}

	private static void assertFailed(final PhasedService lifecycle, final Throwable cause) {
		assertThatThrownBy(() -> lifecycle.awaitTerminated(5, TimeUnit.SECONDS))
				.isInstanceOf(IllegalStateException.class).hasCause(cause);
		assertThat(lifecycle.failureCause()).isSameAs(cause);
	}
}
