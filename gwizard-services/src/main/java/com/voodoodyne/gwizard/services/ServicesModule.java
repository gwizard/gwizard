package com.voodoodyne.gwizard.services;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServicesModule extends AbstractModule {

    @Override
    protected void configure() {
        // create empty multibinding sets in case client code doesn't add 
        // anything to expected multibindings 
        Multibinder.newSetBinder(binder(), Service.class);
        Multibinder.newSetBinder(binder(), Service.Listener.class);
        
        Multibinder.newSetBinder(binder(), ServiceManager.Listener.class)
                .addBinding().to(ServiceManagerListener.class);
        
        bind(ServiceManager.class)
                .toProvider(ServiceManagerProvider.class).in(Singleton.class);

        //
        bind(AppShutdownHandler.class).asEagerSingleton();
    }
}
