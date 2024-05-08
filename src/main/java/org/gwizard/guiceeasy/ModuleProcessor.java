package org.gwizard.guiceeasy;

import com.google.inject.Binding;
import com.google.inject.Injector;
import jakarta.ws.rs.ext.Provider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResourceFactory;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.util.GetRestful;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Looks through the guice injector and registers any jaxrs resources with Resteasy.
 * This code more or less taken as-is from the abandoned resteasy-guice extension.
 */
@RequiredArgsConstructor
@Slf4j
class ModuleProcessor {

	private final Registry registry;
	private final ResteasyProviderFactory providerFactory;

	public void processInjector(final Injector injector) {
		final List<Binding<?>> rootResourceBindings = new ArrayList<>();

		for (final Binding<?> binding : injector.getBindings().values()) {

			final Type type = binding.getKey().getTypeLiteral().getRawType();
			if (type instanceof Class) {
				final Class<?> beanClass = (Class) type;

				if (GetRestful.isRootResource(beanClass)) {
					// deferred registration
					rootResourceBindings.add(binding);
				}

				if (beanClass.isAnnotationPresent(Provider.class)) {
					log.info("registering provider instance for {}", beanClass.getName());
					providerFactory.registerProviderInstance(binding.getProvider().get());
				}
			}
		}

		for (final Binding<?> binding : rootResourceBindings) {
			final Class<?> beanClass = (Class) binding.getKey().getTypeLiteral().getType();
			final ResourceFactory resourceFactory = new GuiceResourceFactory(binding.getProvider(), beanClass);
			log.info("registering factory for {}", beanClass.getName());
			registry.addResourceFactory(resourceFactory);
		}
	}
}
