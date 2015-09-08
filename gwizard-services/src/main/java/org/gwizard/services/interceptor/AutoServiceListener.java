package org.gwizard.services.interceptor;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import org.gwizard.services.Services;

import javax.inject.Provider;

public class AutoServiceListener implements TypeListener {

	private final Provider<Services> servicesProvider;

	public AutoServiceListener(Provider<Services> servicesProvider) {
		this.servicesProvider = servicesProvider;
	}

	@Override
	public void hear(TypeLiteral typeLiteral, TypeEncounter typeEncounter) {
		final Class<?> clazz = typeLiteral.getRawType();
		if (Service.class.isAssignableFrom(clazz)) {
			typeEncounter.register((InjectionListener) i -> servicesProvider.get().add((Service) i));
		} else if (Service.Listener.class.isAssignableFrom(clazz)) {
			typeEncounter.register((InjectionListener) i -> servicesProvider.get().add((Service.Listener) i));
		} else if (ServiceManager.Listener.class.isAssignableFrom(clazz)) {
			typeEncounter.register((InjectionListener) i -> servicesProvider.get().add((ServiceManager.Listener) i));
		}
	}
}
