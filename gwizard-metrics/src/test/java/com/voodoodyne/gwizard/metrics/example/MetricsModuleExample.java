package com.voodoodyne.gwizard.metrics.example;

import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.voodoodyne.gwizard.metrics.MetricsModule;
import com.voodoodyne.gwizard.services.ServicesModule;

public class MetricsModuleExample {


	public static void main(String[] args) throws Exception {
		final Injector injector = Guice.createInjector(
				new ServicesModule(),
				new MetricsModule()
		);

		// start services
		injector.getInstance(ServiceManager.class).startAsync().awaitHealthy();


	}

}
