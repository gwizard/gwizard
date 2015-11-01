package org.gwizard.servlets;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.MoreExecutors;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletTester;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;

public class HealthCheckServletTest extends AbstractServletTest {
    private final HealthCheckRegistry registry = new HealthCheckRegistry();
    private final ExecutorService threadPool = MoreExecutors.newDirectExecutorService();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private HealthCheckServlet healthCheckServlet;

    @Override
    protected void setUp(ServletTester tester) {
        healthCheckServlet = new HealthCheckServlet(registry, objectMapper, threadPool);
        ServletHolder servletHolder = new ServletHolder(healthCheckServlet);
        tester.addServlet(servletHolder, "/healthchecks");
    }

    @Before
    public void setUp() throws Exception {
        request.setMethod("GET");
        request.setURI("/healthchecks");
        request.setVersion("HTTP/1.0");
    }

    @After
    public void tearDown() throws Exception {
        threadPool.shutdown();
    }

    @Test
    public void returns501IfNoHealthChecksAreRegistered() throws Exception {
        processRequest();

        assertThat(response.getStatus())
                .isEqualTo(501);
        assertThat(response.getContent())
                .isEqualTo("{}");
        assertThat(response.get(HttpHeader.CONTENT_TYPE))
                .isEqualTo("application/json");
    }

    @Test
    public void returnsA200IfAllHealthChecksAreHealthy() throws Exception {
        registry.register("fun", new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                return Result.healthy("whee");
            }
        });

        processRequest();

        assertThat(response.getStatus())
                .isEqualTo(200);
        assertThat(response.getContent())
                .isEqualTo("{\"fun\":{\"healthy\":true,\"message\":\"whee\"}}");
        assertThat(response.get(HttpHeader.CONTENT_TYPE))
                .isEqualTo("application/json");
    }

    @Test
    public void returnsA500IfAnyHealthChecksAreUnhealthy() throws Exception {
        registry.register("fun", new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                return Result.healthy("whee");
            }
        });

        registry.register("notFun", new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                return Result.unhealthy("whee");
            }
        });

        processRequest();

        assertThat(response.getStatus())
                .isEqualTo(500);
        assertThat(response.getContent())
                .isEqualTo("{\"fun\":{\"healthy\":true,\"message\":\"whee\"},\"notFun\":{\"healthy\":false,\"message\":\"whee\"}}");
        assertThat(response.get(HttpHeader.CONTENT_TYPE))
                .isEqualTo("application/json");
    }

    @Test
    public void optionallyPrettyPrintsTheJson() throws Exception {
        registry.register("fun", new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                return Result.healthy("whee");
            }
        });

        request.setURI("/healthchecks?pretty=true");

        processRequest();

        assertThat(response.getStatus())
                .isEqualTo(200);
        assertThat(response.getContent())
                .isEqualTo(String.format("{%n" +
                        "  \"fun\" : {%n" +
                        "    \"healthy\" : true,%n" +
                        "    \"message\" : \"whee\"%n" +
                        "  }%n" +
                        "}"));
        assertThat(response.get(HttpHeader.CONTENT_TYPE))
                .isEqualTo("application/json");
    }
}