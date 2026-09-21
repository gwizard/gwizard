package org.gwizard.metrics;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimedInterceptorTest {
	private final MockClock clock = new MockClock();
	private final SimpleMeterRegistry registry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, clock);
	private Injector injector;
	private Example example;

	@BeforeEach
	void setUp() {
		injector = Guice.createInjector(new MetricsModule(), new AbstractModule() {
			@Override
			protected void configure() {
				bind(MeterRegistry.class).toInstance(registry);
				bind(MockClock.class).toInstance(clock);
			}
		});
		example = injector.getInstance(Example.class);
	}

	@AfterEach
	void close() {
		registry.close();
	}

	@Test
	void recordsSuccessWithTagsAndAnnotationOptions() {
		assertThat(example.work()).isEqualTo("done");
		final Timer timer = registry.get("example.work").tags("class", Example.class.getName(), "method", "work",
				"exception", "none", "kind", "test").timer();
		assertThat(timer.count()).isEqualTo(1);
		assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(50);
		assertThat(timer.getId().getDescription()).isEqualTo("Example work");
		assertThat(timer.takeSnapshot().percentileValues()).hasSize(1);
		assertThat(timer.takeSnapshot().histogramCounts()).anySatisfy(bucket -> {
			assertThat(bucket.bucket(TimeUnit.MILLISECONDS)).isEqualTo(100);
			assertThat(bucket.count()).isEqualTo(1);
		});
	}

	@Test
	void recordsAndPreservesCheckedAndUncheckedFailures() {
		for (final Exception failure : new Exception[]{new IOException("checked"), new IllegalStateException("unchecked")}) {
			assertThatThrownBy(() -> example.fail(failure)).isSameAs(failure);
			final Timer timer = registry.get("example.fail").tag("exception", failure.getClass().getSimpleName()).timer();
			assertThat(timer.count()).isEqualTo(1);
			assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(25);
		}
	}

	@Test
	void usesDefaultNameAndIgnoresUnannotatedMethods() {
		example.untimed();
		assertThat(registry.getMeters()).isEmpty();
		example.defaultName();
		assertThat(registry.get("method.timed").tag("method", "defaultName").timer().count()).isEqualTo(1);
	}

	@Test
	void timesClassesWithoutDoubleCountingMethodOverridesOrObjectMethods() {
		final TimedClass timed = injector.getInstance(TimedClass.class);
		timed.hashCode();
		assertThat(registry.getMeters()).isEmpty();
		timed.normal();
		timed.overridden();
		assertThat(registry.get("example.class").tag("method", "normal").timer().count()).isEqualTo(1);
		assertThat(registry.get("example.override").timer().count()).isEqualTo(1);
		assertThat(registry.find("example.class").tag("method", "overridden").timer()).isNull();
	}

	@Test
	void recordsRepeatedAnnotations() {
		example.repeated();
		assertThat(registry.get("example.first").timer().count()).isEqualTo(1);
		assertThat(registry.get("example.second").timer().count()).isEqualTo(1);
	}

	@Test
	void waitsForAsyncCompletionAndPreservesTheOriginalFuture() {
		final CompletableFuture<String> future = new CompletableFuture<>();
		assertThat(example.async(future)).isSameAs(future);
		assertThat(registry.find("example.async").timer()).isNull();
		clock.add(Duration.ofMillis(250));
		future.complete("done");
		final Timer timer = registry.get("example.async").tag("exception", "none").timer();
		assertThat(timer.count()).isEqualTo(1);
		assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(250);
	}

	@Test
	void recordsAsyncFailureCancellationAndNullReturns() {
		final CompletableFuture<String> failed = new CompletableFuture<>();
		example.async(failed);
		failed.completeExceptionally(new CompletionException(new IOException("failed")));
		assertThat(registry.get("example.async").tag("exception", "IOException").timer().count()).isEqualTo(1);
		final CompletableFuture<String> cancelled = new CompletableFuture<>();
		example.async(cancelled);
		cancelled.cancel(false);
		assertThat(registry.get("example.async").tag("exception", "CancellationException").timer().count()).isEqualTo(1);
		assertThat(example.async(null)).isNull();
		assertThat(registry.get("example.async").tag("exception", "none").timer().count()).isEqualTo(1);
	}

	@Test
	void longTasksRemainActiveUntilAsyncCompletion() {
		final CompletableFuture<String> future = new CompletableFuture<>();
		example.longTask(future);
		final LongTaskTimer timer = registry.get("example.long").longTaskTimer();
		assertThat(timer.activeTasks()).isEqualTo(1);
		clock.add(Duration.ofSeconds(2));
		assertThat(timer.duration(TimeUnit.SECONDS)).isEqualTo(2);
		future.completeExceptionally(new IOException("failed"));
		assertThat(timer.activeTasks()).isZero();
	}

	@Test
	void metricsFailuresDoNotReplaceTheApplicationResultOrException() {
		registry.config().onMeterAdded(meter -> { throw new IllegalStateException("Registry failed"); });
		assertThat(example.work()).isEqualTo("done");
		final IOException failure = new IOException("Application failed");
		assertThatThrownBy(() -> example.fail(failure)).isSameAs(failure);
	}

	@Test
	void aBadAnnotationDoesNotPreventTheMethodFromRunning() {
		assertThat(example.invalidLongTask()).isEqualTo("done");
	}

	public static class Example {
		private final MockClock clock;

		@Inject
		public Example(final MockClock clock) {
			this.clock = clock;
		}

		@Timed(value = "example.work", extraTags = {"kind", "test"}, description = "Example work",
				percentiles = {0.95}, histogram = true, serviceLevelObjectives = {0.1, 1.0})
		public String work() {
			clock.add(Duration.ofMillis(50));
			return "done";
		}

		@Timed("example.fail")
		public void fail(final Exception failure) throws Exception {
			clock.add(Duration.ofMillis(25));
			throw failure;
		}

		@Timed
		public void defaultName() {}

		public void untimed() {}

		@Timed("example.first")
		@Timed("example.second")
		public void repeated() {}

		@Timed("example.async")
		public CompletableFuture<String> async(final CompletableFuture<String> future) {
			return future;
		}

		@Timed(value = "example.long", longTask = true)
		public CompletableFuture<String> longTask(final CompletableFuture<String> future) {
			return future;
		}

		@Timed(longTask = true)
		public String invalidLongTask() {
			return "done";
		}
	}

	@Timed("example.class")
	public static class TimedClass {
		public void normal() {}

		@Timed("example.override")
		public void overridden() {}
	}
}
