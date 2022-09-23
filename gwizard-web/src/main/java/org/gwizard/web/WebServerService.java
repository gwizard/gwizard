package org.gwizard.web;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.gwizard.services.Services;

/**
 * A Guava service to startup / shutdown the web server.
 */
@Slf4j
public class WebServerService extends AbstractIdleService {
    private final WebServer server;

    @Inject
    public WebServerService(Services services, WebServer server) {
        this.server = server;

        services.add(this);
    }

    @Override
    protected void startUp() throws Exception {
        log.debug("Starting web server");
        server.start();
    }

    @Override
    protected void shutDown() throws Exception {
        log.debug("Stopping web server");
        server.stop();
    }
}
