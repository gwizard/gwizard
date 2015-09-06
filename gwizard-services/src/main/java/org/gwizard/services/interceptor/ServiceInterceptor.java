package org.gwizard.services.interceptor;

import com.google.common.util.concurrent.Service;
import com.google.inject.Provider;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.gwizard.services.Services;

public class ServiceInterceptor implements MethodInterceptor {

	private final Services services;
	public ServiceInterceptor(Provider<Services> provider) {
		this.services = provider.get();
	}

	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		services.add((Service) methodInvocation.getThis());
		return null;
	}
}
