package org.gwizard.guiceeasy;

import com.google.inject.Provider;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.PropertyInjector;
import org.jboss.resteasy.spi.ResourceFactory;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import java.util.concurrent.CompletionStage;

/**
 * This is taken more or less as-is from the abandoned resteasy-guice extension.
 */
@RequiredArgsConstructor
class GuiceResourceFactory implements ResourceFactory {

	private final Provider provider;

	@Getter
	private final Class<?> scannableClass;

	private PropertyInjector propertyInjector;

	public void registered(final ResteasyProviderFactory factory) {
		propertyInjector = factory.getInjectorFactory().createPropertyInjector(scannableClass, factory);
	}

	@Override
	public Object createResource(final HttpRequest request, final HttpResponse response, final ResteasyProviderFactory factory) {
		final Object resource = provider.get();
		final CompletionStage<Void> propertyStage = propertyInjector.inject(request, response, resource, true);
		return propertyStage == null ? resource : propertyStage.thenApply(v -> resource);
	}

	public void requestFinished(final HttpRequest request, final HttpResponse response, final Object resource) {
	}

	public void unregistered() {
	}
}
