package org.gwizard.test;

import com.google.inject.Module;
import org.gwizard.test.util.EmptyModule;

/**
 * Test classes which use the GuiceExtension must implement this interface to provide
 * a relevant guice configuration for the test.
 */
public interface GuicyTest {
	default Module module() {
		return new EmptyModule();
	}
}
