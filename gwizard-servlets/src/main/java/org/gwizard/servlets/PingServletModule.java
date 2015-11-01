package org.gwizard.servlets;

import com.google.inject.servlet.ServletModule;
import lombok.EqualsAndHashCode;

/***
 * This small ServletModule configures the {@link PingServlet}.
 * It is <b>important</b> that this module be placed before the RestModule
 */
@EqualsAndHashCode(callSuper=false, of={})	// makes installation of this module idempotent
public class PingServletModule extends ServletModule {

    public static final String PING_URI = "/ping";

    private final String prefixUri;

    public PingServletModule(String prefixUri) {
        this.prefixUri = prefixUri;
    }

    @Override
    protected void configureServlets() {
        serve(getUri()).with(PingServlet.class);
    }

    private String getUri() {
        return String.format("%s%s",prefixUri, PING_URI);
    }

}