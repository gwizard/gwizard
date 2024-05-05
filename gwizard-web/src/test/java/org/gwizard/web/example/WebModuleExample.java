package org.gwizard.web.example;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.servlet.ServletModule;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.gwizard.services.Run;
import org.gwizard.web.WebConfig;
import org.gwizard.web.WebModule;

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

	public static void main(String[] args) throws Exception {
		final Injector injector = Guice.createInjector(
						new MyModule(),
						new WebModule());

		injector.getInstance(Run.class).start();
	}
}
