package com.voodoodyne.gwizard.services;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Inject;
import com.google.inject.Provider;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Guice Provider to create the ServiceManager. It is injected with multibound
 * sets of services and listeners. 
 */
@Slf4j
class ServiceManagerProvider implements Provider<ServiceManager> {
    private final Set<Service> services;
    private final Set<ServiceManager.Listener> mgrListeners;
    private final Set<Service.Listener> svcListeners;
    
    @Inject
    public ServiceManagerProvider(
            Set<Service> services, 
            Set<ServiceManager.Listener> mgrListeners,
            Set<Service.Listener> svcListeners) {
        this.services = services;
        this.mgrListeners = mgrListeners;
        this.svcListeners = svcListeners;
    }
    

    @Override
    public ServiceManager get() {
        final ServiceManager serviceMgr = new ServiceManager(services);
        
        // for each service, bind any injected services listeners to it
        for (Service s: services) {
            for (Service.Listener l : svcListeners) {
                s.addListener(l, MoreExecutors.directExecutor());
            }
            
            addLoggingListenerToService(s);
        }
        
        for (ServiceManager.Listener l : mgrListeners) {
            serviceMgr.addListener(l, MoreExecutors.directExecutor());
        }
        
        return serviceMgr;
    }

    /**
     * add a simple logging listener to the given service. The logger used
     * is the service's logger.
     */
    private void addLoggingListenerToService(Service s) {
        // add a simple logging listener
        final Logger serviceLogger = LoggerFactory.getLogger(s.getClass());
        s.addListener(new Service.Listener() {
            @Override
            public void failed(Service.State from, Throwable failure) {
                serviceLogger.warn("failed {}", from.name(), failure);
            }
            
            @Override
            public void terminated(Service.State from) {
                serviceLogger.debug("terminated (from: {})", from);
            }
            
            @Override
            public void stopping(Service.State from) {
                serviceLogger.debug("stopping (from: {})", from);
            }
            
            @Override
            public void running() {
                serviceLogger.debug("running");
            }
            
            @Override
            public void starting() {
                serviceLogger.debug("starting");
            }
        }, MoreExecutors.directExecutor());
    }
    
}
