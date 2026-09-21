package org.gwizard.metrics;

import com.google.inject.Provider;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Guice support for Micrometer's @Timed without requiring Spring or AspectJ. */
@RequiredArgsConstructor
@Slf4j
final class TimedInterceptor implements MethodInterceptor {
	private final Provider<MeterRegistry> registryProvider;

	@Override
	public Object invoke(final MethodInvocation invocation) throws Throwable {
		final Method method = invocation.getMethod();
		Timed[] annotations = method.getAnnotationsByType(Timed.class);
		if (annotations.length == 0) {
			annotations = invocation.getThis().getClass().getAnnotationsByType(Timed.class);
		}

		final MeterRegistry registry = registryProvider.get();
		final Tags tags = Tags.of("class", method.getDeclaringClass().getName(), "method", method.getName());
		final List<Consumer<Throwable>> recordings = new ArrayList<>(annotations.length);
		for (final Timed timed : annotations) {
			try {
				recordings.add(start(registry, timed, tags));
			} catch (RuntimeException failure) {
				log.warn("Unable to start timing {}", method, failure);
			}
		}

		final Object result;
		try {
			result = invocation.proceed();
		} catch (Throwable failure) {
			stop(recordings, failure);
			throw failure;
		}

		if (result instanceof CompletionStage<?> stage) {
			// Observe completion without replacing the future returned to the caller.
			stage.whenComplete((ignored, failure) -> stop(recordings, failure));
		} else {
			stop(recordings, null);
		}
		return result;
	}

	private Consumer<Throwable> start(final MeterRegistry registry, final Timed timed, final Tags tags) {
		if (timed.longTask()) {
			final LongTaskTimer.Sample sample = LongTaskTimer.builder(timed).tags(tags).register(registry).start();
			return failure -> sample.stop();
		}

		final Timer.Builder builder = Timer.builder(timed, "method.timed").tags(tags);
		final Timer.Sample sample = Timer.start(registry);
		return failure -> sample.stop(builder.tag("exception", exceptionName(failure)).register(registry));
	}

	private void stop(final List<Consumer<Throwable>> recordings, final Throwable failure) {
		for (final Consumer<Throwable> recording : recordings) {
			try {
				recording.accept(failure);
			} catch (RuntimeException recordingFailure) {
				// A metrics failure must not replace the application's return value or exception.
				log.warn("Unable to record timing", recordingFailure);
			}
		}
	}

	private String exceptionName(final Throwable failure) {
		if (failure == null) {
			return "none";
		}
		if ((failure instanceof CompletionException || failure instanceof ExecutionException) && failure.getCause() != null) {
			return exceptionName(failure.getCause());
		}
		return failure.getClass().getSimpleName();
	}
}
