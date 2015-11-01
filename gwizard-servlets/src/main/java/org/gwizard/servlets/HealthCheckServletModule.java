package org.gwizard.servlets;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.servlet.ServletModule;
import lombok.EqualsAndHashCode;
import java.util.concurrent.ExecutorService;

/***
 * This small ServletModule configures the {@link MetricsServlet}.
 * It is <b>important</b> that this module be placed before the RestModule
 */
@EqualsAndHashCode(callSuper=false, of={})	// makes installation of this module idempotent
public class HealthCheckServletModule extends ServletModule {

    public static final String HEALTHCHECK_URI = "/healthcheck";

    private final String prefixUri;

    public HealthCheckServletModule(String prefixUri) {
        this.prefixUri = prefixUri;
    }

    @Override
    protected void configureServlets() {
        //Users can override the MetricsFilter binding if they'd like
        OptionalBinder.newOptionalBinder(binder(), ExecutorService.class).setDefault().toInstance(MoreExecutors.newDirectExecutorService());
        serve(getUri()).with(HealthCheckServlet.class);
    }

    private String getUri() {
        return String.format("%s%s",prefixUri, HEALTHCHECK_URI);
    }

}
