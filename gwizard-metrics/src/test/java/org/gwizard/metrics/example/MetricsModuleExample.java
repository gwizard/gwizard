package org.gwizard.metrics.example;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.gwizard.metrics.MetricsModule;
import org.gwizard.services.Run;

public class MetricsModuleExample {


	public static void main(String[] args) throws Exception {
		final Injector injector = Guice.createInjector(
				new MetricsModule()
		);

		// start services
		injector.getInstance(Run.class).start();
	}

}
