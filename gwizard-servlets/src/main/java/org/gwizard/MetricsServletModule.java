package org.gwizard;

import com.codahale.metrics.MetricFilter;
import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.servlet.ServletModule;
import lombok.EqualsAndHashCode;

import static com.codahale.metrics.MetricFilter.ALL;

/***
 * This small ServletModule configures the {@link MetricsServlet}.
 * It is <b>important</b> that this module be placed before the RestModule
 */
@EqualsAndHashCode(callSuper=false, of={})	// makes installation of this module idempotent
public class MetricsServletModule extends ServletModule {

    public static final String METRICS_URI = "/metrics";

    private final String prefixUri;

    public MetricsServletModule(String prefixUri) {
        this.prefixUri = prefixUri;
    }

    @Override
    protected void configureServlets() {
        //Users can override the MetricsFilter binding if they'd like
        OptionalBinder.newOptionalBinder(binder(), MetricFilter.class).setDefault().toInstance(ALL);
        serve(getUri()).with(MetricsServlet.class);
    }

    private String getUri() {
        return String.format("%s%s",prefixUri, METRICS_URI);
    }

}
