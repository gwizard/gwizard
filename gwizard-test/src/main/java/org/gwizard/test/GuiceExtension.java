package org.gwizard.test;

import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;
import com.google.inject.BindingAnnotation;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import jakarta.inject.Qualifier;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Extension which creates a guice injector and puts it in the Store.
 * Test is expected to implement GuicyTest.
 * Does NOT inject on the test object; use GuiceInjectExtension for that.
 *
 * Some of this is borrowed from:
 * https://github.com/JeffreyFalgout/junit5-guice-extension/blob/master/src/main/java/name/falgout/jeffrey/testing/junit5/GuiceExtension.java
 *
 * Note that if you want to use the request scope, you'll need the GuiceWebExtension. And you
 * probably want the GuiceInjectExtension.
 */
public class GuiceExtension implements BeforeEachCallback, ParameterResolver {
	private static final Namespace NAMESPACE = Namespace.create(GuiceExtension.class);

	@Override
	public void beforeEach(final ExtensionContext context) throws Exception {
		final Object testInstanceRaw = context.getTestInstance().get();
		if (!(testInstanceRaw instanceof final GuicyTest testInstance))
			throw new IllegalStateException("Class " + testInstanceRaw.getClass().getName() + " must implement " + GuicyTest.class.getSimpleName());

		final Module module = testInstance.module();

		final Injector injector = Guice.createInjector(module);
		context.getStore(NAMESPACE).put(Injector.class, injector);
	}

	@Override
	public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext) throws ParameterResolutionException {
		final Parameter parameter = parameterContext.getParameter();
		if (getBindingAnnotations(parameter).size() > 1)
			return false;

		final Injector injector = getInjector(extensionContext);
		if (injector == null)
			return false;

		final Key<?> key = getKey(parameter);

		return injector.getExistingBinding(key) != null;
	}

	@Override
	public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext) throws ParameterResolutionException {
		final Parameter parameter = parameterContext.getParameter();
		final Key<?> key = getKey(parameter);
		final Injector injector = getInjector(extensionContext);

		return injector.getInstance(key);

	}

	/** Get the injector from the Store */
	public static Injector getInjector(final ExtensionContext context) {
		return context.getStore(NAMESPACE).get(Injector.class, Injector.class);
	}

	private static List<Annotation> getBindingAnnotations(final AnnotatedElement element) {
		return Arrays.stream(element.getAnnotations())
				.filter(GuiceExtension::isBindingAnnotation)
				.collect(Collectors.toList());
	}

	private static boolean isBindingAnnotation(final Annotation annotation) {
		Class<? extends Annotation> annotationType = annotation.annotationType();
		return annotationType.isAnnotationPresent(Qualifier.class)
				|| annotationType.isAnnotationPresent(BindingAnnotation.class);
	}

	private static Key<?> getKey(Parameter parameter) {
		final TypeToken<?> classType = TypeToken.of(parameter.getDeclaringExecutable().getDeclaringClass());
		final Type resolvedType = classType.resolveType(parameter.getParameterizedType()).getType();

		Optional<Key<?>> key = getOnlyBindingAnnotation(parameter).map(annotation -> Key.get(resolvedType, annotation));
		return key.orElse(Key.get(resolvedType));
	}

	/**
	 * @throws IllegalArgumentException if the given element has more than one binding annotation.
	 */
	private static Optional<? extends Annotation> getOnlyBindingAnnotation(AnnotatedElement element) {
		return Optional.ofNullable(Iterables.getOnlyElement(getBindingAnnotations(element), null));
	}
}
