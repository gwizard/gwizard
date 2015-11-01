package org.gwizard;

import lombok.Data;

import java.util.concurrent.TimeUnit;

/**
 * Standard configuration for the MetricsServlet
 */
@Data
public class MetricsServletConfig {

    /**
     * The time unit for rates at which the metrics should be shown at.
     */
    private TimeUnit rate = TimeUnit.SECONDS;

    /**
     * The time unit for duration at which the metrics should be shown at.
     */
    private TimeUnit duration = TimeUnit.SECONDS;


    private boolean showSamples = false;

    /**
     * Setting this value to anything other than
     * null/empty will set Access-Control-Allow-Origin
     */
    private String allowedOrigin = "";
}
