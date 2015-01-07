package com.voodoodyne.gwizard.jersey;

import com.google.inject.Injector;
import com.squarespace.jersey2.guice.BootstrapUtils;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.hk2.api.ServiceLocator;
import javax.inject.Inject;

/**
 * Binding this as an eager singleton provides the second step of linking Guice back into HK2.
 * (the first step was to install the HK2 BootstrapModule in the Guice module).
 *
 * This needs to happen before anything else related to Jersey starts.
 */
@Slf4j
public class HK2Linker {
	@Inject
	public HK2Linker(Injector injector, ServiceLocator locator) {
		BootstrapUtils.link(locator, injector);
		BootstrapUtils.install(locator);
	}

}
