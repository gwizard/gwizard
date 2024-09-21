package org.gwizard.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.YamlConfigurationFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.validation.Validator;
import java.io.File;
import java.io.IOException;

/**
 * Module which sets up the config file reader. The config class is dynamic.
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

		this.configurationFactory = new YamlConfigurationFactory<>(configClass, validator, objectMapper, propertyPrefix);
		this.configFile = configFile;
	}

	public ConfigProvider(final ConfigModule<T> configModule, final ObjectMapper objectMapper) {
		this(configModule.validator(), objectMapper, configModule.configClass(), configModule.propertyPrefix(), configModule.configFile());
	}

	public T get() {
		try {
			//noinspection unchecked
			return (T)configurationFactory.build(new ImportingConfigurationSourceProvider(), configFile.toString());
		} catch (IOException | ConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Convenience method if we need to read a configuration before the injector is loaded.
	 * For example, maybe we have some config that determines which modules to load.
	 */
	public static <C> C read(final ConfigModule<C> configModule, final ObjectMapper mapper) {
		return new ConfigProvider<C>(configModule, mapper).get();
	}
}