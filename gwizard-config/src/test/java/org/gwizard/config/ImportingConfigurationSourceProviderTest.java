package org.gwizard.config;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ImportingConfigurationSourceProviderTest {

	@Test
	void importsFiles() throws IOException {

		final ImportingConfigurationSourceProvider source = new ImportingConfigurationSourceProvider();

		try (final InputStream stream = source.open("gwizard-config/src/test/resources/parent.yaml")) {
			final JsonNode root = ImportingConfigurationSourceProvider.MAPPER.readTree(stream);

			assertThat(root.path("gwizard")).isEmpty();
			assertThat(root.path("first").asText()).isEqualTo("aaa");
			assertThat(root.path("second").asText()).isEqualTo("bbb1");
			assertThat(root.path("third").asText()).isEqualTo("ccc2");
			assertThat(root.path("fourth").asText()).isEqualTo("ddd2");
			assertThat(root.path("fifth").asText()).isEqualTo("eee1");

			assertThat(root.path("sixth").path("sixA").asText()).isEqualTo("yyy2");
			assertThat(root.path("sixth").path("sixB").asText()).isEqualTo("zzz1");
			assertThat(root.path("sixth").path("sixC").asText()).isEqualTo("www2");

			assertThat(root.path("seventh").asText()).isEqualTo("ggg2");
		}
	}
}