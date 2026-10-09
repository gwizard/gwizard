package org.gwizard.hibernate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.dropwizard.util.Duration;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class DurationConfigTest {

	@Test
	void durationsRoundTripThroughYamlWithoutCustomJacksonModules() throws Exception {
		final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		final DatabaseConfigProperties config = mapper.readValue("""
				pool:
				  connectionTimeout: 2 seconds
				transactionRetry:
				  maxBackoff: 500 ms
				""", DatabaseConfigProperties.class);
		assertThat(config.getPool().getConnectionTimeout()).isEqualTo(Duration.seconds(2));
		assertThat(config.getTransactionRetry().getMaxBackoff()).isEqualTo(Duration.milliseconds(500));
		assertThat(mapper.readValue(mapper.writeValueAsString(config), DatabaseConfigProperties.class)).isEqualTo(config);
	}

	@Test
	void durationConstraintsAreValidatedAlongsideOtherConfiguration() {
		try (var factory = Validation.buildDefaultValidatorFactory()) {
			final var validator = factory.getValidator();
			final DatabaseConfigProperties config = new DatabaseConfigProperties();
			assertThat(validator.validate(config)).isEmpty();
			config.getPool().setConnectionTimeout(Duration.microseconds(249_999));
			config.getTransactionRetry().setMaxBackoff(Duration.microseconds(999));
			assertThat(validator.validate(config)).extracting(v -> v.getPropertyPath().toString())
					.containsExactlyInAnyOrder("pool.connectionTimeout", "transactionRetry.maxBackoff");
			config.getPool().setConnectionTimeout(null);
			config.getTransactionRetry().setMaxBackoff(null);
			assertThat(validator.validate(config)).extracting(v -> v.getPropertyPath().toString())
					.containsExactlyInAnyOrder("pool.connectionTimeout", "transactionRetry.maxBackoff");
		}
	}

	@Test
	void poolValidationAlsoWorksWithoutBeanValidation() {
		final ConnectionPoolConfig config = new ConnectionPoolConfig();
		for (Duration duration : Arrays.asList(null, Duration.seconds(-1), Duration.milliseconds(0), Duration.microseconds(249_999))) {
			config.setConnectionTimeout(duration);
			assertThatIllegalArgumentException().isThrownBy(config::validate).withMessageContaining("pool.connectionTimeout");
		}
		config.setConnectionTimeout(Duration.microseconds(250_000));
		config.validate();
	}
}
