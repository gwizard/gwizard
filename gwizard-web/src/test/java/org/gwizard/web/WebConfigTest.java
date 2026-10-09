package org.gwizard.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.dropwizard.util.Duration;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class WebConfigTest {

	@Test
	void yamlDurationReachesJettyInMilliseconds() throws Exception {
		final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		final WebConfigProperties defaults = mapper.readValue("{}", WebConfigProperties.class);
		assertThat(defaults.getPort()).isEqualTo(8080);
		assertThat(defaults.getShutdownTimeout()).isEqualTo(Duration.seconds(30));
		assertThat(new WebConfig() {}.getShutdownTimeout()).isEqualTo(defaults.getShutdownTimeout());
		final WebConfigProperties config = mapper.readValue("shutdownTimeout: 2 minutes", WebConfigProperties.class);
		try (var fixture = new WebLifecycleTest.Fixture(false, config.getShutdownTimeout())) {
			fixture.start();
			assertThat(fixture.web.jetty.getStopTimeout()).isEqualTo(120_000);
		}
	}

	@Test
	void zeroDisablesDraining() throws Exception {
		try (var fixture = new WebLifecycleTest.Fixture(false, Duration.seconds(0))) {
			fixture.start();
			assertThat(fixture.web.jetty.getStopTimeout()).isZero();
		}
	}

	@Test
	void rejectsMissingNegativeAndSubMillisecondTimeoutsBeforeCreatingServer() {
		for (Duration duration : Arrays.asList(null, Duration.seconds(-1), Duration.nanoseconds(-1), Duration.microseconds(999))) {
			final WebServer server = new WebServer(new WebConfigProperties(0, duration), null, null);
			assertThatIllegalArgumentException().isThrownBy(server::start).withMessageContaining("shutdownTimeout");
		}
	}
}
