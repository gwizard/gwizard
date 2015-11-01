package org.gwizard.servlets;

import com.google.inject.servlet.ServletModule;
import lombok.EqualsAndHashCode;

/***
 * This small ServletModule configures the {@link ThreadDumpServlet}.
 * It is <b>important</b> that this module be placed before the RestModule
 */
@EqualsAndHashCode(callSuper=false, of={})	// makes installation of this module idempotent
public class ThreadDumpServletModule extends ServletModule {

    public static final String THREADS_URI = "/threads";

    private final String prefixUri;

    public ThreadDumpServletModule(String prefixUri) {
        this.prefixUri = prefixUri;
    }

    @Override
    protected void configureServlets() {
        //Users can override the MetricsFilter binding if they'd like
        serve(getUri()).with(ThreadDumpServlet.class);
    }

    private String getUri() {
        return String.format("%s%s",prefixUri, THREADS_URI);
    }

}
