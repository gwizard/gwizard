package org.gwizard.services;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
public class ServicesModuleTests {

	public static class NoOpService extends AbstractIdleService {
		@Override
		protected void startUp() throws Exception {}
		@Override
		protected void shutDown() throws Exception {}
	}

	protected Injector injector;

	protected ServiceManager mgr;

	protected Service testService1;
	protected Service testService2;

	@Mock
	protected Service.Listener mockListener;
	@Mock
	protected ServiceManager.Listener mockMgrListener;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.initMocks(this);

		testService1 = new NoOpService();
		testService2 = new NoOpService();

		injector = Guice.createInjector(new ServicesModule());

		injector.getInstance(Services.class).add(testService1);
		injector.getInstance(Services.class).add(testService2);
		injector.getInstance(Services.class).add(mockListener);
		injector.getInstance(Services.class).add(mockMgrListener);

		mgr = injector.getInstance(ServiceManager.class);
	}

	@AfterEach
	public void teardown() {
		mgr.stopAsync().awaitStopped();
	}

	@Test
	public void serviceMgrAndServicesRunning() {
		mgr.startAsync().awaitHealthy();
		assertThat(mgr.isHealthy()).isEqualTo(true);
		assertThat(testService1.state()).isEqualTo(Service.State.RUNNING);
		assertThat(testService2.state()).isEqualTo(Service.State.RUNNING);
	}

	@Test
	public void serviceManagerListenerWasCalledOnce() {
		mgr.startAsync().awaitHealthy();
		verify(mockMgrListener, times(1)).healthy();
	}

	@Test
	public void serviceListenerCalledForEachService() {
		mgr.startAsync().awaitHealthy();
		verify(mockListener, times(2)).starting();
	}


}
