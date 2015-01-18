package com.voodoodyne.gwizard.web;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;

/**
 * A Guava service to startup / shutdown the web server.
 */
@Slf4j
public class WebServerLaunchingService extends AbstractExecutionThreadService {
    private final CountDownLatch doneSignal = new CountDownLatch(1);
    private final WebServer server;

    @Inject
    public WebServerLaunchingService(WebServer server) {
        this.server = server;
    }

    @Override
    protected void startUp() throws Exception {
        // actual startup is done in run(), since it may take a while
    }

    @Override
    protected void run() throws Exception {
        log.debug("Starting web server");
        server.start();

        // wait until ServiceManager signals us to stop
        doneSignal.await();

        log.debug("Stopping web server");
        server.stop();
    }

    @Override
    protected void shutDown() throws Exception {
    }

    @Override
    protected void triggerShutdown() {
        log.debug("Triggering shutdown");
        doneSignal.countDown();
    }
}
