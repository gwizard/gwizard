package com.voodoodyne.gwizard.hibernate;

import lombok.Data;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties related to the database
 */
@Data
public class DbConfig {

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
