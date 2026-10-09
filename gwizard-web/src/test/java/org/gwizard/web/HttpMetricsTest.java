package org.gwizard.web;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(20)
class HttpMetricsTest {
	@Test
	void recordsStatusesAndAsyncCompletionWithoutRawUrlTagsAndRemovesMetersOnStop() throws Exception {
		final var registry = new SimpleMeterRegistry();
		try {
			try (var fixture = new WebLifecycleTest.Fixture(true, 5_000, 0, registry)) {
				fixture.start();
				assertThat(registry.get("jetty.threads.jobs").gauge().value()).isNotNegative();
				assertThat(registry.get("jetty.threads.config.max").gauge().value()).isPositive();
				for (String path : new String[] {"/missing/123?secret=one", "/missing/456?secret=two"})
					assertThat(fixture.get(path).get(5, TimeUnit.SECONDS).statusCode()).isEqualTo(404);
				await(() -> count(registry, "404") == 2);
				assertThat(registry.get("http.server.requests").timers()).hasSize(1);
				assertThat(registry.get("http.server.requests").timer().getId().getTags())
						.extracting(tag -> tag.getKey()).containsExactlyInAnyOrder("method", "status", "outcome", "error");

				final var request = fixture.get("/work");
				assertThat(fixture.servlet.entered.await(5, TimeUnit.SECONDS)).isTrue();
				assertThat(registry.get("http.server.requests.active").gauge().value()).isEqualTo(1);
				assertThat(count(registry, "200")).isZero();
				final long heldAt = System.nanoTime();
				Thread.sleep(30);
				final long heldNanos = System.nanoTime() - heldAt;
				fixture.servlet.complete();
				assertThat(request.get(5, TimeUnit.SECONDS).statusCode()).isEqualTo(200);
				await(() -> count(registry, "200") == 1);
				assertThat(registry.get("http.server.requests.active").gauge().value()).isZero();
				assertThat(registry.get("http.server.requests").tags("method", "GET", "status", "200", "outcome", "SUCCESS")
						.timer().totalTime(TimeUnit.NANOSECONDS)).isGreaterThanOrEqualTo(heldNanos);
			}
			assertThat(registry.getMeters()).isEmpty();
			assertThat(registry.isClosed()).isFalse();
		} finally {
			registry.close();
		}
	}

	@Test
	void coversAdditionalContextsAndShutdownRejections() throws Exception {
		final var registry = new SimpleMeterRegistry();
		try (var fixture = new WebLifecycleTest.Fixture(true, 5_000, 0, registry)) {
			fixture.start();
			final var request = fixture.get("/extra/");
			assertThat(fixture.extra.entered.await(5, TimeUnit.SECONDS)).isTrue();
			assertThat(registry.get("http.server.requests.active").gauge().value()).isEqualTo(1);
			fixture.manager.stopAsync();
			fixture.awaitDraining();
			assertThat(fixture.get("/work").get(5, TimeUnit.SECONDS).statusCode()).isEqualTo(503);
			await(() -> count(registry, "503") == 1);
			assertThat(registry.get("http.server.requests").tags("status", "503", "outcome", "SERVER_ERROR").timer().count()).isEqualTo(1);
			assertThat(registry.get("http.server.requests.active").gauge().value()).isEqualTo(1);
			fixture.extra.complete();
			assertThat(request.get(5, TimeUnit.SECONDS).body()).isEqualTo("extra");
		} finally {
			registry.close();
		}
	}

	@Test
	void recordingFailureDoesNotBreakTheResponseOrLeakActiveRequests() throws Exception {
		final var registry = new SimpleMeterRegistry();
		try {
			registry.config().meterFilter(new MeterFilter() {
				@Override public Meter.Id map(final Meter.Id id) {
					if (id.getName().equals("http.server.requests"))
						throw new IllegalStateException("metrics unavailable");
					return id;
				}
			});
			try (var fixture = new WebLifecycleTest.Fixture(true, 5_000, 0, registry)) {
				fixture.start();
				final var request = fixture.get("/work");
				assertThat(fixture.servlet.entered.await(5, TimeUnit.SECONDS)).isTrue();
				fixture.servlet.complete();
				assertThat(request.get(5, TimeUnit.SECONDS).statusCode()).isEqualTo(200);
				await(() -> registry.get("http.server.requests.active").gauge().value() == 0);
			}
		} finally {
			registry.close();
		}
	}

	private static long count(final SimpleMeterRegistry registry, final String status) {
		return registry.find("http.server.requests").tag("status", status).timers().stream().mapToLong(Timer::count).sum();
	}

	private static void await(final BooleanSupplier ready) throws Exception {
		final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (!ready.getAsBoolean() && System.nanoTime() < deadline)
			Thread.sleep(5);
		assertThat(ready.getAsBoolean()).isTrue();
	}
}
