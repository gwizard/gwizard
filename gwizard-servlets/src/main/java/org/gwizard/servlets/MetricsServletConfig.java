package org.gwizard.servlets;

import lombok.Data;
import lombok.experimental.Builder;

import java.util.concurrent.TimeUnit;

/**
 * Standard configuration for the MetricsServlet
 */
@Data
@Builder
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

    //Create the Lombok builder class so we can set sane defaults
    public static class MetricsServletConfigBuilder {
        private TimeUnit rate = TimeUnit.SECONDS;
        private TimeUnit duration = TimeUnit.SECONDS;
        private boolean showSamples = false;
        private String allowedOrigin = "";
    }
}
