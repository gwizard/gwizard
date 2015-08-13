package org.gwizard.web.example;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.gwizard.services.Run;
import org.gwizard.web.ServletContextConfigurator;
import org.gwizard.web.WebConfig;
import org.gwizard.web.WebModule;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.servlet.ServletModule;

import ch.qos.logback.access.PatternLayoutEncoder;
import ch.qos.logback.access.jetty.RequestLogImpl;
import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.core.ConsoleAppender;

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
			cfg.setPort(8889);    // default is 8080
			return cfg;
		}
	}

	@Singleton
	private static class ConfigureServletContext implements ServletContextConfigurator {
		@Inject
		private ConfigureServletContext() {}

		@Override
		public void configure(ServletContextHandler sch) {
			//			sch.setSessionHandler(new SessionHandler(riqJettySessionManager));
			sch.setResourceBase("src/main/webapp");

			Server server = sch.getServer();
			final HandlerCollection handlers = new HandlerCollection();

			handlers.addHandler(new INeedToBeFirstHandler());

			// ensure any other handlers we already had are also present
			handlers.addHandler(server.getHandler());

			server.setHandler(handlers);
		}
	}

	public static class INeedToBeFirstHandler extends AbstractHandler {

		@Override
		public void handle(String s,
				Request request,
				HttpServletRequest httpServletRequest,
				HttpServletResponse httpServletResponse) throws IOException, ServletException {
			// for example rewrites need to be the first handler in the chain
			System.out.println("handled!");
		}
	}

	public static class ExampleModule extends AbstractModule {

		@Override
		protected void configure() {
			bind(RequestLogHandler.class).toInstance(getRequestLogHandler());
			bind(ConfigureServletContext.class);
		}

		private RequestLogHandler getRequestLogHandler() {
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
			return requestLogHandler;
		}
	}

	public static void main(String[] args) throws Exception {
		final Injector injector = Guice.createInjector(
				new ExampleModule(),
				new MyModule(),
				new WebModule());

		injector.getInstance(Run.class).start();
	}
}
