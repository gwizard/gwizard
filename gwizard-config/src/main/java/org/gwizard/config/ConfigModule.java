package org.gwizard.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import jakarta.inject.Singleton;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * Module which sets up the configuration object.
 *
 * @param <T> is the type of the configuration object.
 */
@Slf4j
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper=false, of={})	// makes installation of this module idempotent
public class ConfigModule<T> extends AbstractModule {
	private final Class<T> configClass;
	private final String propertyPrefix;
	private final File configFile;

	/**
	 * We will eventually support multiple config files, so that needs to go last.
	 */
	@Deprecated
	public ConfigModule(final File configFile, final Class<T> configClass, final String propertyPrefix) {
		this(configClass, propertyPrefix, configFile);
	}

	/**
	 * We will eventually support multiple config files, so that needs to go last.
	 */
	@Deprecated
	public ConfigModule(final File configFile, final Class<T> configClass) {
		this(configClass, configFile);
	}

	/**
	 * Defaults to the property prefix of "gw"
	 */
	public ConfigModule(final Class<T> configClass, final File configFile) {
		this(configClass, "gw", configFile);
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