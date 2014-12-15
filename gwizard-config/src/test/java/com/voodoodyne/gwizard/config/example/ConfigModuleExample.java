package com.voodoodyne.gwizard.config.example;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.voodoodyne.gwizard.config.ConfigModule;
import lombok.Data;
import java.io.File;

/**
 * Self-contained example of using the ConfigModule by itself.
 *
 * Values can be overridden with system properties, for example:
 *
 * -Dgw.name=OtherName
 *
 * The prefix can be changed with an optional parameter to ConfigModule's constructor.
 */
public class ConfigModuleExample {
	/** Make up your own config file format */
	@Data
	public static class MyConfig {
		private String name;
		private int size;
	}

	/** Expects the first command line arg to be the path to a config yml file */
	public static void main(String[] args) throws Exception {
		final File configFile = new File(args[0]);
		final ConfigModule configModule = new ConfigModule(configFile, MyConfig.class);	// could pass in prefix too
		final Injector injector = Guice.createInjector(configModule);

		// MyConfig is bound as a singleton and can be injected anywhere
		final MyConfig config = injector.getInstance(MyConfig.class);

		System.out.println("Name is " + config.getName() + ", size is " + config.getSize());
	}
}
