package com.voodoodyne.gwizard.services.example;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.voodoodyne.gwizard.logging.LoggingModule;
import com.voodoodyne.gwizard.services.ServicesModule;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;

/**
 * Self-contained example of using the ServicesModule by itself.
 */
@Slf4j
public class ServicesModuleExample {
    
    @Singleton
    @Slf4j
    public static class ExampleServiceListener extends Service.Listener {

        @Override
        public void failed(Service.State from, Throwable failure) {
            log.info("failed ({})", from, failure);
        }

        @Override
        public void terminated(Service.State from) {
            log.info("terminated ({})", from);
        }

        @Override
        public void stopping(Service.State from) {
            log.info("stopping ({})", from);
        }

        @Override
        public void running() {
            log.info("running");
        }

        @Override
        public void starting() {
            log.info("starting");
        }
        
    }

    /**
     * an example service that has minimal app startup/shutdown requirements
     */
    @Singleton
    @Slf4j
    public static class ExampleService extends AbstractIdleService {
        @Override
        protected void startUp() throws Exception {
            log.info("This is where my service does something at startup");
        }

        @Override
        protected void shutDown() throws Exception {
            log.info("This is where my service does something at shutdown");
        }
    }

    /**
     * an example for how you might create a service for a module who has
     * non-trivial startup costs (e.g. any I/O)
     */
    @Singleton
    @Slf4j
    public static class ExampleLongStartupService extends AbstractExecutionThreadService {
        private final CountDownLatch doneSignal = new CountDownLatch(1);

        @Override
        protected void startUp() throws Exception {
            log.info("This is where my service does something at startup");
        }

        @Override
        protected void run() throws Exception {
            log.info("Here's where my service would do some heavy lifting");

            // wait until signaled to shutdown
            doneSignal.await();

            log.info("Finally done! whew! what a relief");
        }

        @Override
        protected void shutDown() throws Exception {
            log.info("This is where my service does something at shutdown");
        }

        @Override
        protected void triggerShutdown() {
            doneSignal.countDown();
        }
    }
    
    public static class ExampleModule extends AbstractModule {
        @Override
        protected void configure() {
            Multibinder.newSetBinder(binder(), Service.class)
                    .addBinding().to(ExampleService.class);
            Multibinder.newSetBinder(binder(), Service.class)
                    .addBinding().to(ExampleLongStartupService.class);
            
            Multibinder.newSetBinder(binder(), Service.Listener.class)
                    .addBinding().to(ExampleServiceListener.class);
        }
    }

    public static void main(String[] args) throws Exception {
        final Injector injector = Guice.createInjector(
                new LoggingModule(),
                new ServicesModule(),
                new ExampleModule()
        );

        // start services
        injector.getInstance(ServiceManager.class).startAsync().awaitHealthy();


        // here's how you'd stop services
        // injector.getInstance(ServiceManager.class).stopAsync().awaitStopped(5, TimeUnit.SECONDS);

        // or, you can just exit, and the services will get a shutdown signal
        //System.exit(0);

        // or, just leave things running and wait for cntrl-C or other exit to kill the application
    }
}
