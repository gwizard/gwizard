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

/**
 * An HTTP servlets which outputs a {@code text/plain} {@code "pong"} response.
 */
@Singleton
public class PingServlet extends HttpServlet {
    private static final String CONTENT = "pong";
    private static final String NO_CACHE = "must-revalidate,no-cache,no-store";

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setHeader(HttpHeaders.CACHE_CONTROL, NO_CACHE);
        resp.setContentType(MediaType.TEXT_PLAIN);
        try (final PrintWriter writer = resp.getWriter()) {
            writer.println(CONTENT);
        }
    }
}