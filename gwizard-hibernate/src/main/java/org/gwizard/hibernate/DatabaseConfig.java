package org.gwizard.hibernate;

import java.util.Collections;
import java.util.Map;

/**
 * Configuration properties related to the database. The default values are inadequate to bootstrap
 * a real database; you must provide an initialized version of this in your own module.
 */
public interface DatabaseConfig {

	String getDriverClass();

	String getUrl();

	default String getUser() { return null; }

	default String getPassword() { return null; }

	default Map<String, String> getProperties() {
		return Collections.emptyMap();
	}
}
