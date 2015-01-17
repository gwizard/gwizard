package com.voodoodyne.gwizard.services.example;

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
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

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
        }

        @Override
        public void terminated(Service.State from) {
        }

        @Override
        public void stopping(Service.State from) {
        }

        @Override
        public void running() {
        }

        @Override
        public void starting() {
        }
        
    }
    
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
    
    public static class ExampleModule extends AbstractModule {
        @Override
        protected void configure() {
            Multibinder.newSetBinder(binder(), Service.class)
                    .addBinding().to(ExampleService.class);
            
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

        // start services, but wait no longer than 5 secs
        injector.getInstance(ServiceManager.class).startAsync().awaitHealthy(5, TimeUnit.SECONDS);

        
        // stop services, but wait no longer than 5 secs
        injector.getInstance(ServiceManager.class).stopAsync().awaitStopped(5, TimeUnit.SECONDS);
    }
}
