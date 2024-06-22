package org.gwizard.hibernate;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Optional implementation of DatabaseConfig suitable for reading from a yaml file.
 */
@Data
@NoArgsConstructor
public class DatabaseConfigProperties implements DatabaseConfig {

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
	public DatabaseConfigProperties(final String driverClass, final String url, final String user, final String password) {
		this.driverClass = driverClass;
		this.url = url;
		this.user = user;
		this.password = password;
	}
}
