/*
 */

package org.gwizard.rest;

import com.google.common.collect.Lists;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;


/**
 * Some utilities for working with types
 */
public class Types
{
	/**
	 * If a class is annotated with this, it will not be included in Types.getTypes()
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.TYPE})
	public @interface IgnoreType {}

	/**
	 * Get a list of all types of the thing, excluding Object
	 */
	public static List<String> getTypes(final Object thing) {
		return getTypes(thing, Object.class);
	}

	/**
	 * Get a list of all types of the thing, up until (exclusve) any of the stopClasses
	 * @param stopClasses can be empty
	 */
	public static List<String> getTypes(final Object thing, final Class<?>... stopClasses) {
		List<String> types = Lists.newArrayList();
		addType(thing.getClass(), types, stopClasses);
		return types;
	}

	/** Recursive implementation of getTypesExclusive() */
	private static void addType(final Class<?> clazz, final List<String> list, final Class<?>[] stopClasses) {
		if (!contains(stopClasses, clazz)) {
			if (getAnnotation(IgnoreType.class, clazz.getDeclaredAnnotations()) == null)
				list.add(clazz.getSimpleName());

			addType(clazz.getSuperclass(), list, stopClasses);
		}
	}

	private static boolean contains(final Object[] array, final Object objectToFind) {
		for (final Object obj : array) {
			if (obj.equals(objectToFind)) {
				return true;
			}
		}

		return false;
	}

	/** @return the annotation with the specified class, or null if not present in the array */
	public static Annotation getAnnotation(final Class<? extends Annotation> type, final Annotation[] annos) {
		for (int i=0; i<annos.length; i++)
			if (annos[i].getClass() == type)
				return annos[i];

		return null;
	}
}