package org.gwizard.test;

import com.google.inject.Injector;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Separate out the actual injection of things from the guice setup so that other extensions do setup
 * that might affect what gets injected.
 */
public class GuiceInjectExtension implements BeforeEachCallback {
	@Override
	public void beforeEach(final ExtensionContext context) throws Exception {
		final Injector injector = GuiceExtension.getInjector(context);
		final Object testInstance = context.getTestInstance().get();

		injector.injectMembers(testInstance);
	}
}
