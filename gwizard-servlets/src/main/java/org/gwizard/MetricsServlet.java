package org.gwizard;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.OutputStream;


/**
 * A very simple servlet which returns the metrics in a given registry
 * as an {@code application/json} response.
 *
 * It is based on the Dropwizard equivalent however less functional (no advanced search).
 * You can provide some additional properties within the {@link MetricsServletConfig}
 *
 * @see <a href="https://github.com/dropwizard/metrics/blob/master/metrics-servlets/src/main/java/io/dropwizard/metrics/servlets/MetricsServlet.java">MetricsServlet.java</a>
 */
@Singleton
public class MetricsServlet extends HttpServlet {

    private static final String NO_CACHE = "must-revalidate,no-cache,no-store";

    private final MetricRegistry registry;
    private final ObjectMapper mapper;
    private final MetricsServletConfig metricsConfig;
    private final MetricFilter filter;

    @Inject
    public MetricsServlet(MetricRegistry registry, ObjectMapper mapper,
                          MetricsServletConfig metricsConfig, MetricFilter filter) {
        this.registry = registry;
        this.mapper = mapper;
        this.metricsConfig = metricsConfig;
        this.filter = filter;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        mapper.registerModule(new MetricsModule(metricsConfig.getRate(),
                metricsConfig.getDuration(),
                metricsConfig.isShowSamples(),
                filter));
    }

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType(MediaType.APPLICATION_JSON);
        if (!Strings.isNullOrEmpty(metricsConfig.getAllowedOrigin())) {
            resp.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, metricsConfig.getAllowedOrigin());
        }
        resp.setHeader(HttpHeaders.CACHE_CONTROL, NO_CACHE);
        resp.setStatus(HttpServletResponse.SC_OK);

        try(final OutputStream output = resp.getOutputStream()){
            this.getWriter(req).writeValue(output, this.registry);
        }

    }

    private ObjectWriter getWriter(HttpServletRequest request) {
        final boolean prettyPrint = Boolean.parseBoolean(request.getParameter("pretty"));
        if (prettyPrint) {
            return mapper.writerWithDefaultPrettyPrinter();
        }
        return mapper.writer();
    }


}