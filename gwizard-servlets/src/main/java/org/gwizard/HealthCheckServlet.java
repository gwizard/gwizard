package org.gwizard;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.json.HealthCheckModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.net.HttpHeaders;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ExecutorService;

@Singleton
public class HealthCheckServlet extends HttpServlet {

    private static final String NO_CACHE = "must-revalidate,no-cache,no-store";

    private final HealthCheckRegistry registry;
    private final ExecutorService executorService;
    private final ObjectMapper mapper;

    @Inject
    public HealthCheckServlet(HealthCheckRegistry registry, ObjectMapper mapper,ExecutorService executorService) {
        this.registry = registry;
        this.mapper = mapper;
        this.executorService = executorService;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.mapper.registerModule(new HealthCheckModule());
    }

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {
        final SortedMap<String, HealthCheck.Result> results = runHealthChecks();
        resp.setContentType(MediaType.APPLICATION_JSON);
        resp.setHeader(HttpHeaders.CACHE_CONTROL, NO_CACHE);

        if (results.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
        } else {
            if (isAllHealthy(results)) {
                resp.setStatus(HttpServletResponse.SC_OK);
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }

        try (final OutputStream output = resp.getOutputStream())
        {
            getWriter(req).writeValue(output, results);
        }

    }

    private ObjectWriter getWriter(HttpServletRequest request) {
        final boolean prettyPrint = Boolean.parseBoolean(request.getParameter("pretty"));
        if (prettyPrint) {
            return mapper.writerWithDefaultPrettyPrinter();
        }
        return mapper.writer();
    }

    private SortedMap<String, HealthCheck.Result> runHealthChecks() {
        return registry.runHealthChecks(executorService);
    }

    private static boolean isAllHealthy(Map<String, HealthCheck.Result> results) {
        return results.values().stream().allMatch( r -> r.isHealthy() );
    }
}