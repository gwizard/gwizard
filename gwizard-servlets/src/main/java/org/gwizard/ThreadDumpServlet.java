package org.gwizard;


import com.codahale.metrics.jvm.ThreadDump;
import com.google.common.net.HttpHeaders;

import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;

/**
 * An HTTP servlets which outputs a {@code text/plain} dump of all threads in
 * the VM. Only responds to {@code GET} requests.
 */
@Singleton
public class ThreadDumpServlet extends HttpServlet {

    private static final String NO_CACHE = "must-revalidate,no-cache,no-store";

    private transient ThreadDump threadDump;

    @Override
    public void init() throws ServletException {
        try {
            // Some PaaS like Google App Engine blacklist java.lang.managament
            this.threadDump = new ThreadDump(ManagementFactory.getThreadMXBean());
        } catch (NoClassDefFoundError ncdfe) {
            this.threadDump = null; // we won't be able to provide thread dump
        }
    }

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(MediaType.TEXT_PLAIN);
        resp.setHeader(HttpHeaders.CACHE_CONTROL, NO_CACHE);
        if (threadDump == null) {
            resp.getWriter().println("Sorry your runtime environment does not allow to dump threads.");
            return;
        }
        try (final OutputStream output = resp.getOutputStream()) {
            threadDump.dump(output);
        }
    }

}
