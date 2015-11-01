package org.gwizard.servlets;

import com.google.inject.servlet.ServletModule;
import lombok.EqualsAndHashCode;

/***
 * This small ServletModule configures the {@link AdminServletModule}.
 * It is <b>important</b> that this module be placed before the RestModule
 *
 * This is helper servlet that binds all the other servlet modules and provides
 * a very simple administration menu to view the links.
 *
 * @see HealthCheckServlet
 * @see MetricsServlet
 * @see PingServlet
 * @see ThreadDumpServlet
 */
@EqualsAndHashCode(callSuper=false, of={})	// makes installation of this module idempotent
public class AdminServletModule extends ServletModule {

    public static final String ADMIN_URI = "/admin";

    private final String uriPrefix;

    public AdminServletModule() {
        this(ADMIN_URI);
    }

    public AdminServletModule(String adminUri) {
        this.uriPrefix = adminUri;
    }

    @Override
    protected void configureServlets() {
        install(new HealthCheckServletModule(uriPrefix));
        install(new MetricsServletModule(uriPrefix));
        install(new PingServletModule(uriPrefix));
        install(new ThreadDumpServletModule(uriPrefix));
        serve(ADMIN_URI).with(AdminServlet.class);
    }

}