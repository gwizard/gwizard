package com.voodoodyne.gwizard.hibernate;

import lombok.Data;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties related to the database. The default values are inadequate to bootstrap
 * a real database; you must provide an initialized version of this in your own module.
 */
@Data
public class DatabaseConfig {

	@NotNull
	private String driverClass;

	@NotNull
	private String user = null;

	private String password = "";

	@NotNull
	private String url = null;

	@NotNull
	private Map<String, String> properties = new HashMap<>();
}
