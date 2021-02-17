package org.gwizard.hibernate;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties related to the database. The default values are inadequate to bootstrap
 * a real database; you must provide an initialized version of this in your own module.
 */
@Data
@NoArgsConstructor
public class DatabaseConfig {

	@NotNull
	private String driverClass;

	@NotNull
	private String url = null;

	@NotNull
	private String user = null;

	private String password = "";

	@NotNull
	private Map<String, String> properties = new HashMap<>();

	/** */
	public DatabaseConfig(final String driverClass, final String url, final String user, final String password) {
		this.driverClass = driverClass;
		this.url = url;
		this.user = user;
		this.password = password;
	}
}
