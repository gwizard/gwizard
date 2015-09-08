package org.gwizard.services.interceptor;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.gwizard.services.Services;
import org.gwizard.services.ServicesModule;
import org.testng.annotations.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AutoServiceTest {

	@Slf4j
	@Singleton
	@AutoService
	static class ExampleServiceListener extends Service.Listener {}

	@Slf4j
	@Singleton
	@AutoService
	static class ExampleService extends AbstractIdleService {
		@Override
		protected void startUp() throws Exception {
			log.info("This is where my service does something at startup");
		}

		@Override
		protected void shutDown() throws Exception {
			log.info("This is where my service does something at shutdown");
		}
	}

	@Slf4j
	@Singleton
	@AutoService
	static class ExampleServiceManagerListener extends ServiceManager.Listener {}

	@Test
	public void shouldRegisterServicesAndListeners() {
		// given
		Services mockServices = mock(Services.class);

		// when
		Guice.createInjector(
				new AbstractModule() {
					@Override
					protected void configure() {
						bind(Services.class).toInstance(mockServices);
						bind(ExampleService.class).asEagerSingleton();
						bind(ExampleServiceListener.class).asEagerSingleton();
						bind(ExampleServiceManagerListener.class).asEagerSingleton();
					}
				},
				new ServicesModule()
		);

		// then
		verify(mockServices).add(any(Service.class));
		verify(mockServices).add(any(Service.Listener.class));
		verify(mockServices).add(any(ServiceManager.Listener.class));
	}
}