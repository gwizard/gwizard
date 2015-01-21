package com.voodoodyne.gwizard.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import javax.inject.Singleton;
import javax.validation.Validation;
import javax.validation.Validator;
import java.io.File;

/**
 * Module which sets up the configuration object. That object can be anything derived from the Config class.
 */
@RequiredArgsConstructor
@Slf4j
@EqualsAndHashCode(of={})	// makes installation of this module idempotent
public class ConfigModule extends AbstractModule {
	private final File configFile;
	private final Class<?> configClass;
	private final String propertyPrefix;

	public ConfigModule(File configFile, Class<?> configClass) {
		this(configFile, configClass, "gw");
	}

	@Override
	protected void configure() {
		// Binding a dynamic class is surprisingly complicated
		bind((Class<Object>)configClass).toProvider(new TypeLiteral<ConfigProvider<Object>>(){}).in(Singleton.class);
	}

	@Provides
	@PropertyPrefix
	public String propertyPrefix() {
		return propertyPrefix;
	}

	@Provides
	@ConfigClass
	public Class<?> configClass() {
		return configClass;
	}

	@Provides
	@ConfigFile
	public File configFile() {
		return configFile;
	}

	@Provides
	@Singleton
	public Validator validator() { return Validation.buildDefaultValidatorFactory().getValidator(); }
}