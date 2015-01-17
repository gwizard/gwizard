package com.voodoodyne.gwizard.services;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class ServiceManagerListener extends ServiceManager.Listener {
    @Override
    public void failure(Service service) {
        log.error("service failure: {}", service.toString(), service.failureCause());
    }

    @Override
    public void stopped() {
        log.trace("all services stopped");
    }

    @Override
    public void healthy() {
        log.trace("all services healthy");
    }
}
