package org.gwizard;

import com.google.common.net.HttpHeaders;

import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import static org.gwizard.HealthCheckServletModule.HEALTHCHECK_URI;
import static org.gwizard.MetricsServletModule.METRICS_URI;
import static org.gwizard.PingServletModule.PING_URI;
import static org.gwizard.ThreadDumpServletModule.THREADS_URI;

@Singleton
public class AdminServlet extends HttpServlet {

    private static final String TEMPLATE = String.format(
            "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"%n" +
                    "        \"http://www.w3.org/TR/html4/loose.dtd\">%n" +
                    "<html>%n" +
                    "<head>%n" +
                    "  <title>Administration</title>%n" +
                    "</head>%n" +
                    "<body>%n" +
                    "  <h1>Operational Menu</h1>%n" +
                    "  <ul>%n" +
                    "    <li><a href=\"{0}{1}?pretty=true\">Metrics</a></li>%n" +
                    "    <li><a href=\"{2}{3}\">Ping</a></li>%n" +
                    "    <li><a href=\"{4}{5}\">Threads</a></li>%n" +
                    "    <li><a href=\"{6}{7}?pretty=true\">Healthcheck</a></li>%n" +
                    "  </ul>%n" +
                    "</body>%n" +
                    "</html>"
    );

    private static final String NO_CACHE = "must-revalidate,no-cache,no-store";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String path = req.getContextPath() + req.getServletPath();

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setHeader(HttpHeaders.CACHE_CONTROL,NO_CACHE);
        resp.setContentType(MediaType.TEXT_HTML);
        try (final PrintWriter writer = resp.getWriter())
        {
            writer.println(MessageFormat.format(TEMPLATE, path, METRICS_URI, path, PING_URI, path,
                    THREADS_URI, path, HEALTHCHECK_URI));
        }
    }

}