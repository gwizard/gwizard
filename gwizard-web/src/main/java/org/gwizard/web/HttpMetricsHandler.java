package org.gwizard.web;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jetty.JettyServerThreadPoolMetrics;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.EventsHandler;
import org.eclipse.jetty.util.NanoTime;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Measures completed exchanges, including asynchronous responses and graceful-shutdown rejections. */
final class HttpMetricsHandler extends EventsHandler {
	private final MeterRegistry registry;
	private final AtomicInteger active = new AtomicInteger();
	private final Set<Meter> meters = ConcurrentHashMap.newKeySet();
	private JettyServerThreadPoolMetrics threadPoolMetrics;

	HttpMetricsHandler(final Handler handler, final MeterRegistry registry) {
		super(handler);
		this.registry = registry;
	}

	@Override
	protected void doStart() throws Exception {
		try {
			meters.add(Gauge.builder("http.server.requests.active", active, AtomicInteger::get)
					.description("Requests being handled, including asynchronous responses").register(registry));
			threadPoolMetrics = new JettyServerThreadPoolMetrics(getServer().getThreadPool(), Tags.empty());
			threadPoolMetrics.bindTo(registry);
			super.doStart();
		} catch (Exception | Error failure) {
			try {
				removeMeters();
			} catch (Exception cleanup) {
				failure.addSuppressed(cleanup);
			}
			throw failure;
		}
	}

	@Override
	protected void onBeforeHandling(final Request request) {
		active.incrementAndGet();
	}

	@Override
	protected void onComplete(final Request request, final int status, final HttpFields headers, final Throwable failure) {
		active.decrementAndGet();
		// Never tag with raw URLs or arbitrary HTTP methods supplied by clients.
		final HttpMethod method = HttpMethod.fromString(request.getMethod());
		final Timer timer = Timer.builder("http.server.requests")
				.description("Completed HTTP exchanges, including time waiting for dispatch and response completion")
				.tags("method", method == null ? "OTHER" : method.asString(),
						"status", status >= 100 && status <= 599 ? Integer.toString(status) : "UNKNOWN",
						"outcome", outcome(status), "error", failure == null ? "false" : "true")
				.register(registry);
		meters.add(timer);
		timer.record(NanoTime.since(request.getBeginNanoTime()), TimeUnit.NANOSECONDS);
	}

	private static String outcome(final int status) {
		return switch (status / 100) {
			case 1 -> "INFORMATIONAL";
			case 2 -> "SUCCESS";
			case 3 -> "REDIRECTION";
			case 4 -> "CLIENT_ERROR";
			case 5 -> "SERVER_ERROR";
			default -> "UNKNOWN";
		};
	}

	@Override
	protected void doStop() throws Exception {
		try {
			super.doStop();
		} finally {
			removeMeters();
		}
	}

	private void removeMeters() throws Exception {
		try {
			if (threadPoolMetrics != null)
				threadPoolMetrics.close();
		} finally {
			meters.forEach(registry::remove);
			meters.clear();
		}
	}
}
