package org.gwizard.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.matcher.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogCallInterceptorTest {

	private final Logger logger = (Logger)LoggerFactory.getLogger(Example.class);
	private final Logger interceptorLogger = (Logger)LoggerFactory.getLogger(LogCallInterceptor.class);
	private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
	private Level originalLevel;
	private Level originalInterceptorLevel;
	private boolean originalAdditive;
	private Example example;

	@BeforeEach
	void setUp() {
		originalLevel = logger.getLevel();
		originalInterceptorLevel = interceptorLogger.getLevel();
		originalAdditive = logger.isAdditive();
		logger.setLevel(Level.INFO);
		interceptorLogger.setLevel(Level.OFF);
		logger.setAdditive(false);
		appender.setContext(logger.getLoggerContext());
		appender.start();
		logger.addAppender(appender);
		example = interceptedExample(new LogCallInterceptor());
	}

	private Example interceptedExample(final LogCallInterceptor interceptor) {
		return Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bindInterceptor(Matchers.any(), Matchers.annotatedWith(LogCall.class), interceptor);
			}
		}).getInstance(Example.class);
	}

	@AfterEach
	void restoreLogger() {
		logger.detachAppender(appender);
		appender.stop();
		logger.setLevel(originalLevel);
		interceptorLogger.setLevel(originalInterceptorLevel);
		logger.setAdditive(originalAdditive);
	}

	@Test
	void disabledDebugDoesNotFormatArgumentsOrResultAndStillCallsTheMethod() {
		interceptorLogger.setLevel(Level.DEBUG); // Only the application's logger should control interception.
		final Object argument = new ThrowingToString();
		final Object result = new ThrowingToString();

		assertThat(example.echo(argument, result)).isSameAs(result);
		assertThat(example.calls).isEqualTo(1);
		assertThat(appender.list).isEmpty();
	}

	@Test
	void disabledDebugPreservesTheOriginalExceptionWithoutFormattingIt() {
		final RuntimeException failure = new RuntimeException() {
			@Override public String toString() { throw new AssertionError("Exception must not be formatted"); }
		};
		example.failure = failure;

		assertThatThrownBy(example::fail).isInstanceOf(RuntimeException.class).isSameAs(failure);
		assertThat(appender.list).isEmpty();
	}

	@Test
	void enabledDebugStillLogsArgumentsAndResult() {
		logger.setLevel(Level.DEBUG);

		assertThat(example.echo("hello", null)).isNull();
		assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
				.containsExactly("Calling echo(\"hello\", null)", "Returning null");
		assertThat(appender.list).extracting(ILoggingEvent::getLoggerName)
				.containsOnly(Example.class.getName());
		assertThat(appender.list).extracting(ILoggingEvent::getLevel).containsOnly(Level.DEBUG);
	}

	@Test
	void enabledDebugHonorsDisabledResultLogging() {
		logger.setLevel(Level.DEBUG);
		example.result = new ThrowingToString();

		assertThat(example.hiddenResult()).isSameAs(example.result);
		assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
				.containsExactly("Calling hiddenResult()");
	}

	@Test
	void enabledDebugStillLogsAndPropagatesExceptions() {
		logger.setLevel(Level.DEBUG);
		example.failure = new IllegalStateException("failure");

		assertThatThrownBy(example::fail).isSameAs(example.failure);
		assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
				.containsExactly("Calling fail()", "Threw java.lang.IllegalStateException: failure");
	}

	@Test
	void configuredLevelAppliesToCallsReturnsAndExceptions() {
		for (org.slf4j.event.Level level : org.slf4j.event.Level.values()) {
			final Level logbackLevel = Level.toLevel(level.name());
			logger.setLevel(logbackLevel);
			appender.list.clear();
			final Example configured = interceptedExample(new LogCallInterceptor(level));
			assertThat(configured.echo("hello", null)).isNull();
			configured.result = new ThrowingToString();
			assertThat(configured.hiddenResult()).isSameAs(configured.result);
			configured.failure = new IllegalStateException("failure");
			assertThatThrownBy(configured::fail).isSameAs(configured.failure);

			assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
					.containsExactly("Calling echo(\"hello\", null)", "Returning null", "Calling hiddenResult()",
							"Calling fail()", "Threw java.lang.IllegalStateException: failure");
			assertThat(appender.list).extracting(ILoggingEvent::getLevel).containsOnly(logbackLevel);
			assertThat(appender.list).extracting(ILoggingEvent::getLoggerName).containsOnly(Example.class.getName());
		}
	}

	@Test
	void disabledConfiguredLevelSkipsFormattingEvenWhenDebugIsEnabled() {
		logger.setLevel(Level.DEBUG);
		final Example configured = interceptedExample(new LogCallInterceptor(org.slf4j.event.Level.TRACE));
		final Object result = new ThrowingToString();
		assertThat(configured.echo(new ThrowingToString(), result)).isSameAs(result);
		assertThat(configured.calls).isEqualTo(1);
		configured.failure = new RuntimeException() {
			@Override public String toString() { throw new AssertionError("Exception must not be formatted"); }
		};
		assertThatThrownBy(configured::fail).isSameAs(configured.failure);
		assertThat(appender.list).isEmpty();
	}

	static class ThrowingToString {
		@Override public String toString() { throw new AssertionError("Value must not be formatted"); }
	}

	static class Example {
		int calls;
		Object result;
		RuntimeException failure;

		@LogCall
		public Object echo(final Object argument, final Object result) {
			calls++;
			return result;
		}

		@LogCall(result = false)
		public Object hiddenResult() {
			return result;
		}

		@LogCall
		public void fail() {
			throw failure;
		}
	}
}
