package com.google.inject.persist.jpa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.dropwizard.util.Duration;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.RollbackException;
import org.gwizard.hibernate.DatabaseConfigProperties;
import org.gwizard.hibernate.TransactionRetryConfig;
import org.junit.jupiter.api.Test;

import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.Arrays;

import static com.google.inject.persist.jpa.TransactionRetryPolicy.isSerializationFailure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class TransactionRetryPolicyTest {

	@Test
	void readsYamlAndRetainsDefaultsWhenOmitted() throws Exception {
		final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		final DatabaseConfigProperties defaults = mapper.readValue("{}", DatabaseConfigProperties.class);
		assertThat(defaults.getTransactionRetry().getMaxAttempts()).isEqualTo(5);
		assertThat(defaults.getTransactionRetry().getMaxBackoff().toMilliseconds()).isEqualTo(250);

		final DatabaseConfigProperties configured = mapper.readValue("""
				transactionRetry:
				  maxAttempts: 3
				  maxBackoff: 1 second
				""", DatabaseConfigProperties.class);
		assertThat(configured.getTransactionRetry().getMaxAttempts()).isEqualTo(3);
		assertThat(configured.getTransactionRetry().getMaxBackoff().toMilliseconds()).isEqualTo(1000);
	}

	@Test
	void backoffGrowsToCapIndependentlyOfAttemptBudget() {
		final TransactionRetryConfig config = new TransactionRetryConfig();
		config.setMaxBackoff(Duration.milliseconds(256));
		final TransactionRetryPolicy policy = new TransactionRetryPolicy(config);
		assertThat(new long[] {policy.backoffCeilingMillis(1), policy.backoffCeilingMillis(2),
				policy.backoffCeilingMillis(3), policy.backoffCeilingMillis(4),
				policy.backoffCeilingMillis(5), policy.backoffCeilingMillis(100)})
				.containsExactly(16, 32, 64, 128, 256, 256);
		config.setMaxAttempts(100);
		final TransactionRetryPolicy longer = new TransactionRetryPolicy(config);
		for (int retry = 1; retry <= 10; retry++) {
			assertThat(longer.backoffCeilingMillis(retry)).isEqualTo(policy.backoffCeilingMillis(retry));
			assertThat(policy.backoffMillis(retry)).isBetween(0L, policy.backoffCeilingMillis(retry));
		}
	}

	@Test
	void tinyAndHugeCapsDoNotOverflow() {
		final TransactionRetryConfig config = new TransactionRetryConfig();
		config.setMaxBackoff(Duration.milliseconds(1));
		assertThat(new TransactionRetryPolicy(config).backoffCeilingMillis(1)).isEqualTo(1);
		config.setMaxBackoff(Duration.milliseconds(Long.MAX_VALUE));
		final TransactionRetryPolicy policy = new TransactionRetryPolicy(config);
		assertThat(policy.backoffCeilingMillis(Integer.MAX_VALUE)).isEqualTo(Long.MAX_VALUE);
		assertThat(policy.backoffMillis(5)).isBetween(0L, Long.MAX_VALUE);
	}

	@Test
	void validatesConfiguration() {
		assertThatIllegalArgumentException().isThrownBy(() -> new TransactionRetryPolicy(null));
		final TransactionRetryConfig config = new TransactionRetryConfig();
		config.setMaxAttempts(0);
		assertThatIllegalArgumentException().isThrownBy(() -> new TransactionRetryPolicy(config))
				.withMessageContaining("maxAttempts");
		config.setMaxAttempts(1);
		for (Duration duration : Arrays.asList(null, Duration.seconds(-1), Duration.milliseconds(0), Duration.microseconds(999))) {
			config.setMaxBackoff(duration);
			assertThatIllegalArgumentException().isThrownBy(() -> new TransactionRetryPolicy(config))
					.withMessageContaining("maxBackoff");
		}
	}

	@Test
	void findsWrappedAndBatchedSerializationFailures() {
		final SQLException sql = new SQLException("a database-specific message", "40001");
		assertThat(isSerializationFailure(new RollbackException(new PersistenceException(sql)))).isTrue();
		final BatchUpdateException batch = new BatchUpdateException();
		batch.setNextException(sql);
		assertThat(isSerializationFailure(new PersistenceException(batch))).isTrue();
	}

	@Test
	void ignoresOtherStatesMessagesAndSuppressedExceptions() {
		for (String state : new String[] {null, "40000", "40P01", "23505", "08006"})
			assertThat(isSerializationFailure(new SQLException("40001", state))).isFalse();
		final RuntimeException primary = new RuntimeException("40001");
		primary.addSuppressed(new SQLException("cleanup failed", "40001"));
		assertThat(isSerializationFailure(primary)).isFalse();
	}

	@Test
	void handlesCyclesInCauseAndJdbcChains() {
		final SQLException first = new SQLException("first");
		final SQLException second = new SQLException("second");
		first.initCause(second);
		second.initCause(first);
		first.setNextException(second);
		second.setNextException(first);
		assertThat(isSerializationFailure(first)).isFalse();
	}
}
