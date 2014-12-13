package com.voodoodyne.gwizard.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.validation.Validator;
import java.io.File;
import java.io.IOException;

/**
 * Module which sets up the config file reader. The config class might actually be a custom user subtype.
 */
@RequiredArgsConstructor
@Slf4j
public class ConfigProvider<T> implements Provider<T> {

	private final ConfigurationFactory<?> configurationFactory;
	private final File configFile;

	@Inject
	public ConfigProvider(
			Validator validator,
			ObjectMapper objectMapper,
			@ConfigClass Class<?> configClass,
			@PropertyPrefix String propertyPrefix,
			@ConfigFile File configFile) {

		this.configurationFactory = new ConfigurationFactory<>(configClass, validator, objectMapper, propertyPrefix);
		this.configFile = configFile;
	}

	public T get() {
		if (!configFile.exists())
			throw new IllegalStateException("No such config file " + configFile);

		try {
			return (T)configurationFactory.build(configFile);
		} catch (IOException | ConfigurationException e) {
			throw new RuntimeException(e);
		}
	}
}