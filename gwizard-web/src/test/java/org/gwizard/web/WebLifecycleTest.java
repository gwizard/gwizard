package org.gwizard.web;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.ServletModule;
import io.dropwizard.util.Duration;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.GracefulHandler;
import org.eclipse.jetty.util.Callback;
import org.gwizard.services.Services;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.net.URI;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Timeout(20)
class WebLifecycleTest {

	static class Dependency extends AbstractService {
		@Override protected void doStart() {}
		@Override protected void doStop() { notifyStopped(); }
		@Override protected void doCancelStart() { notifyStopped(); }
		void ready() { notifyStarted(); }
	}

	@Singleton
	static class ObservedWebServer extends WebServer {
		volatile Server jetty;
		@Inject
		ObservedWebServer(final WebConfig config, final EventListenerScanner listeners, final HandlerScanner handlers) {
			super(config, listeners, handlers);
		}
		@Override
		protected Server createServer(final WebConfig config) {
			jetty = super.createServer(config);
			((ServerConnector)jetty.getConnectors()[0]).setHost("127.0.0.1");
			return jetty;
		}
	}

	static class WorkServlet extends HttpServlet {
		final Dependency dependency;
		final boolean asynchronous;
		final CountDownLatch entered = new CountDownLatch(1);
		final CountDownLatch release = new CountDownLatch(1);
		volatile AsyncContext async;
		volatile boolean destroyedWhileDependencyRunning;
		WorkServlet(final Dependency dependency, final boolean asynchronous) {
			this.dependency = dependency;
			this.asynchronous = asynchronous;
		}
		@Override
		protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
			if (asynchronous) {
				async = request.startAsync();
				async.setTimeout(0);
				entered.countDown();
			} else {
				entered.countDown();
				try {
					if (!release.await(10, TimeUnit.SECONDS))
						throw new IOException("Test request was never released");
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IOException(e);
				}
				response.getWriter().write("dependency running: " + dependency.isRunning());
			}
		}
		void complete() throws IOException {
			release.countDown();
			final AsyncContext pending = async;
			async = null;
			if (pending != null) {
				pending.getResponse().getWriter().write("dependency running: " + dependency.isRunning());
				pending.complete();
			}
		}
		@Override
		public void destroy() {
			destroyedWhileDependencyRunning = dependency.isRunning();
		}
	}

	static class ExtraHandler extends Handler.Abstract {
		final CountDownLatch entered = new CountDownLatch(1);
		Response response;
		Callback callback;
		@Override
		public boolean handle(final Request request, final Response response, final Callback callback) {
			this.response = response;
			this.callback = callback;
			entered.countDown();
			return true;
		}
		void complete() {
			if (callback != null) {
				response.write(true, ByteBuffer.wrap("extra".getBytes(StandardCharsets.UTF_8)), callback);
				callback = null;
			}
		}
	}

	static class Fixture implements AutoCloseable {
		final Dependency dependency = new Dependency();
		final WorkServlet servlet;
		final ExtraHandler extra = new ExtraHandler();
		final ServiceManager manager;
		final ObservedWebServer web;
		final HttpClient client = HttpClient.newHttpClient();
		Fixture(final boolean asynchronous, final Duration drainTimeout) {
			this(asynchronous, drainTimeout, 0);
		}
		Fixture(final boolean asynchronous, final Duration drainTimeout, final int port) {
			this(asynchronous, drainTimeout, port, null);
		}
		Fixture(final boolean asynchronous, final Duration drainTimeout, final int port, final MeterRegistry registry) {
			servlet = new WorkServlet(dependency, asynchronous);
			final ContextHandler context = new ContextHandler(extra, "/extra");
			final Injector injector = Guice.createInjector(new WebModule(), new ServletModule() {
				@Override protected void configureServlets() {
					if (registry != null)
						bind(MeterRegistry.class).toInstance(registry);
					bind(WebConfig.class).toInstance(new WebConfigProperties(port, drainTimeout));
					bind(WebServer.class).to(ObservedWebServer.class);
					bind(Handler.class).toInstance(context);
					bind(WorkServlet.class).toInstance(servlet);
					serve("/work").with(WorkServlet.class);
				}
			});
			web = injector.getInstance(ObservedWebServer.class);
			injector.getInstance(Services.class).add(dependency);
			manager = injector.getInstance(ServiceManager.class);
		}
		void start() throws Exception {
			manager.startAsync();
			assertThat(web.jetty).isNull();
			dependency.ready();
			manager.awaitHealthy(5, TimeUnit.SECONDS);
		}
		CompletableFuture<HttpResponse<String>> get(final String path) {
			final int port = ((ServerConnector)web.jetty.getConnectors()[0]).getLocalPort();
			return client.sendAsync(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).build(),
					HttpResponse.BodyHandlers.ofString());
		}
		void awaitDraining() throws Exception {
			final Handler root = web.jetty.getHandler();
			final GracefulHandler graceful = (GracefulHandler)(root instanceof HttpMetricsHandler metrics ? metrics.getHandler() : root);
			final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			while (!graceful.isShutdown() && System.nanoTime() < deadline)
				Thread.sleep(5);
			assertThat(graceful.isShutdown()).isTrue();
		}
		@Override
		public void close() throws Exception {
			try {
				extra.complete();
				servlet.complete();
			} catch (IllegalStateException | IOException ignored) {
				// A test that exhausts the drain timeout has already closed the async request.
			} finally {
				manager.stopAsync().awaitStopped(10, TimeUnit.SECONDS);
			}
		}
	}

	@Test
	void drainsServletAndAdditionalContextBeforeStoppingDependencies() throws Exception {
		try (Fixture fixture = new Fixture(false, Duration.seconds(5))) {
			fixture.start();
			final var request = fixture.get("/work");
			final var extraRequest = fixture.get("/extra/");
			assertThat(fixture.servlet.entered.await(5, TimeUnit.SECONDS)).isTrue();
			assertThat(fixture.extra.entered.await(5, TimeUnit.SECONDS)).isTrue();

			fixture.manager.stopAsync();
			fixture.awaitDraining();
			assertThat(fixture.manager.isHealthy()).isFalse();
			assertThat(fixture.dependency.isRunning()).isTrue();
			assertThat(fixture.get("/work").get(5, TimeUnit.SECONDS).statusCode()).isEqualTo(503);
			fixture.extra.complete();
			assertThat(extraRequest.get(5, TimeUnit.SECONDS).body()).isEqualTo("extra");
			assertThat(fixture.dependency.isRunning()).isTrue();
			fixture.servlet.complete();
			final var response = request.get(5, TimeUnit.SECONDS);
			assertThat(response.statusCode()).isEqualTo(200);
			assertThat(response.body()).isEqualTo("dependency running: true");
			fixture.manager.awaitStopped(5, TimeUnit.SECONDS);
			assertThat(fixture.servlet.destroyedWhileDependencyRunning).isTrue();
			assertThat(fixture.dependency.state()).isEqualTo(Service.State.TERMINATED);
		}
	}

	@Test
	void asyncRequestCanUseMoreThanJettysOneSecondShutdownIdleTimeout() throws Exception {
		try (Fixture fixture = new Fixture(true, Duration.seconds(5))) {
			fixture.start();
			final var request = fixture.get("/work");
			assertThat(fixture.servlet.entered.await(5, TimeUnit.SECONDS)).isTrue();
			fixture.manager.stopAsync();
			fixture.awaitDraining();
			assertThatThrownBy(() -> request.get(1_200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
			assertThat(fixture.dependency.isRunning()).isTrue();
			fixture.servlet.complete();
			assertThat(request.get(5, TimeUnit.SECONDS).body()).isEqualTo("dependency running: true");
			fixture.manager.awaitStopped(5, TimeUnit.SECONDS);
		}
	}

	@Test
	void drainTimeoutClosesHungRequestAndStillStopsDependencies() throws Exception {
		try (Fixture fixture = new Fixture(true, Duration.milliseconds(100))) {
			fixture.start();
			fixture.get("/work");
			assertThat(fixture.servlet.entered.await(5, TimeUnit.SECONDS)).isTrue();
			fixture.manager.stopAsync().awaitStopped(5, TimeUnit.SECONDS);
			assertThat(fixture.web.jetty.isStopped()).isTrue();
			assertThat(fixture.dependency.state()).isEqualTo(Service.State.TERMINATED);
		}
	}

	@Test
	void stoppingDuringStartupNeverOpensWebListener() throws Exception {
		try (Fixture fixture = new Fixture(false, Duration.seconds(5))) {
			fixture.manager.startAsync();
			fixture.manager.stopAsync().awaitStopped(5, TimeUnit.SECONDS);
			assertThat(fixture.web.jetty).isNull();
			assertThat(fixture.dependency.state()).isEqualTo(Service.State.TERMINATED);
		}
	}

	@Test
	void failedJettyStartupCleansUpStartedContexts() throws Exception {
		try (ServerSocket occupied = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
			 Fixture fixture = new Fixture(false, Duration.seconds(5), occupied.getLocalPort())) {
			// Exercise WebServer directly so this expected failure does not invoke process exit.
			assertThatThrownBy(fixture.web::start).isInstanceOf(IOException.class);
			assertThat(fixture.web.jetty.isStopped()).isTrue();
			assertThat(fixture.extra.isStopped()).isTrue();
		}
	}
}
