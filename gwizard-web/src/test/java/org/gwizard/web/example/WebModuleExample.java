package org.gwizard.web.example;

import ch.qos.logback.access.PatternLayout;
import ch.qos.logback.access.jetty.RequestLogImpl;
import ch.qos.logback.access.PatternLayoutEncoder;
import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.servlet.ServletModule;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.gwizard.services.Run;
import org.gwizard.web.WebConfig;
import org.gwizard.web.WebModule;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Self-contained example of using the WebModule by itself.
 */
public class WebModuleExample {
	@Singleton
	public static class HelloServlet extends HttpServlet {
		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.getWriter().println("Hello, World");
		}
	}

	public static class MyModule extends ServletModule {
		@Override
		protected void configureServlets() {
			serve("/hello").with(HelloServlet.class);
		}

		@Provides
		@Singleton
		public WebConfig webConfig() {
			WebConfig cfg = new WebConfig();
			cfg.setPort(8888);	// default is 8080
			return cfg;
		}
	}

	public static class TestHandlerModule extends AbstractModule {

		@Override
		protected void configure() {
			RequestLogImpl requestLog = new RequestLogImpl();

			ConsoleAppender<IAccessEvent> consoleAppender = new ConsoleAppender<>();
			consoleAppender.setContext(requestLog);
			consoleAppender.setName("console");

			PatternLayoutEncoder patternLayout = new PatternLayoutEncoder();
			patternLayout.setContext(requestLog);
			patternLayout.setPattern("%h %l %u %user %date \"%r\" %s %b");
			patternLayout.start();

			consoleAppender.setEncoder(patternLayout);
			consoleAppender.start();

			requestLog.addAppender(consoleAppender);

			RequestLogHandler requestLogHandler = new RequestLogHandler();
			requestLogHandler.setRequestLog(requestLog);

			bind(RequestLogHandler.class).toInstance(requestLogHandler);
		}
	}

	public static void main(String[] args) throws Exception {
		final Injector injector = Guice.createInjector(
						new TestHandlerModule(),
						new MyModule(),
						new WebModule());

		injector.getInstance(Run.class).start();
	}
}
