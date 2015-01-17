package com.voodoodyne.gwizard.services;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.multibindings.Multibinder;
import lombok.extern.slf4j.Slf4j;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
public class ServiceModuleTests {
    
    public static class NoOpIdleService extends AbstractIdleService {
        @Override
        protected void startUp() throws Exception {
        }

        @Override
        protected void shutDown() throws Exception {
        }
    }

    protected Injector injector;
    
    protected ServiceManager mgr;
    
    protected Service testService1;
    
    protected Service testService2;
    
    @Mock
    protected Service.Listener mockListener;
    
    @Mock
    protected ServiceManager.Listener mockMgrListener;
    
    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
    
        testService1 = new NoOpIdleService();
    
        testService2 = new NoOpIdleService();

        injector = Guice.createInjector(
                new ServicesModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        Multibinder.newSetBinder(binder(), Service.class)
                                .addBinding().toInstance(testService1);
                        
                        Multibinder.newSetBinder(binder(), Service.class)
                                .addBinding().toInstance(testService2);

                        Multibinder.newSetBinder(binder(), Service.Listener.class)
                                .addBinding().toInstance(mockListener);

                        Multibinder.newSetBinder(binder(), ServiceManager.Listener.class)
                                .addBinding().toInstance(mockMgrListener);
                    }
                });
        
        
        mgr = injector.getInstance(ServiceManager.class);
    }
    
    @AfterMethod
    public void teardown() {
        mgr.stopAsync().awaitStopped();
    }
    
    
    @Test
    public void serviceMgrAndServicesRunning() {
        mgr.startAsync().awaitHealthy();
        assertThat(mgr.isHealthy(), equalTo(true));
        assertThat(testService1.state(), equalTo(Service.State.RUNNING));
        assertThat(testService2.state(), equalTo(Service.State.RUNNING));
    }
    
    @Test
    public void mgrListenerWasCalledOnce() {
        mgr.startAsync().awaitHealthy();
        verify(mockMgrListener, times(1)).healthy();
    }
    
    @Test
    public void svcListenerCalledForEachService() {
        mgr.startAsync().awaitHealthy();
        verify(mockListener, times(2)).starting();
    }
    
    
}
