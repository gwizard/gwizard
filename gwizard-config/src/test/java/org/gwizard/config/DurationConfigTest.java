package org.gwizard.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.util.Duration;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DurationConfigTest {

	@Data
	public static class Config {
		private Duration timeout = Duration.seconds(1);
	}

	@TempDir
	Path directory;

	@Test
	void readsHumanReadableDurationsAndSystemPropertyOverrides() throws Exception {
		final Path yaml = directory.resolve("config.yaml");
		Files.writeString(yaml, "timeout: 30 seconds\n");
		final ConfigModule<Config> module = new ConfigModule<>(Config.class, "durationTest", yaml.toFile());
		final ObjectMapper mapper = new ObjectMapper();
		assertThat(ConfigProvider.read(module, mapper).getTimeout()).isEqualTo(Duration.seconds(30));
		final String previous = System.setProperty("durationTest.timeout", "250 ms");
		try {
			assertThat(ConfigProvider.read(module, mapper).getTimeout()).isEqualTo(Duration.milliseconds(250));
		} finally {
			if (previous == null)
				System.clearProperty("durationTest.timeout");
			else
				System.setProperty("durationTest.timeout", previous);
		}
	}
}
