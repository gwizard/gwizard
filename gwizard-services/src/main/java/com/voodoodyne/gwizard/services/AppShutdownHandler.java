package com.voodoodyne.gwizard.services;

import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Provider;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Registers a shutdown hook that will stop any running services.
 *
 * <p/>bound as an eager singleton in ServicesModule
 */
@Slf4j
public class AppShutdownHandler {
    public static final int DEFAULT_STOP_TIMEOUT = 5;
    private final Provider<ServiceManager> serviceManager;

    @Inject
    public AppShutdownHandler(Provider<ServiceManager> serviceManager) {
        this.serviceManager = serviceManager;

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                handleShutdown();
            }
        });
    }

    public void handleShutdown() {
        try {
            log.debug("Shutting down services...");
            // Give the services no more than 5 seconds to stop
            serviceManager.get().stopAsync().awaitStopped(DEFAULT_STOP_TIMEOUT, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            log.error("Timeout waiting for service shutdown", ex);
        }
    }

}
