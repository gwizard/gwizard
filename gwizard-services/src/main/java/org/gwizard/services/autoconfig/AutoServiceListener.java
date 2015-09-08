package org.gwizard.services.autoconfig;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import org.gwizard.services.Services;

import javax.inject.Provider;

class AutoServiceListener implements TypeListener {

	private final Provider<Services> servicesProvider;

	public AutoServiceListener(Provider<Services> servicesProvider) {
		this.servicesProvider = servicesProvider;
	}

	@Override
	public <I> void hear(TypeLiteral<I> typeLiteral, TypeEncounter<I> typeEncounter) {
		final Class<?> clazz = typeLiteral.getRawType();
		if (Service.class.isAssignableFrom(clazz)) {
			typeEncounter.register((InjectionListener<I>) i -> servicesProvider.get().add((Service)i));
		} else if (Service.Listener.class.isAssignableFrom(clazz)) {
			typeEncounter.register((InjectionListener<I>) i -> servicesProvider.get().add((Service.Listener)i));
		} else if (ServiceManager.Listener.class.isAssignableFrom(clazz)) {
			typeEncounter.register((InjectionListener<I>) i -> servicesProvider.get().add((ServiceManager.Listener) i));
		}
	}
}
