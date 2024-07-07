package org.gwizard.test.util;

import com.google.common.collect.Maps;
import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Scopes;

import java.util.Map;

import static com.google.common.base.Preconditions.checkState;

/**
 * Simplified from https://github.com/google/guice/wiki/CustomScopes
 */
public class SimpleScope implements Scope {

	private final ThreadLocal<Map<Key<?>, Object>> values = new ThreadLocal<>();

	public void enter() {
		checkState(values.get() == null, "A scoping block is already in progress");
		values.set(Maps.newHashMap());
	}

	public void exit() {
		checkState(values.get() != null, "No scoping block in progress");
		values.remove();
	}

	public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
		return () -> {
			Map<Key<?>, Object> scopedObjects = getScopedObjects(key);

			@SuppressWarnings("unchecked")
			T current = (T) scopedObjects.get(key);
			if (current == null && !scopedObjects.containsKey(key)) {
				current = unscoped.get();

				// don't remember proxies; these exist only to serve circular dependencies
				if (Scopes.isCircularProxy(current)) {
					return current;
				}

				scopedObjects.put(key, current);
			}
			return current;
		};
	}

	private <T> Map<Key<?>, Object> getScopedObjects(Key<T> key) {
		Map<Key<?>, Object> actualValues = this.values.get();
		if (actualValues == null) {
			throw new OutOfScopeException("Cannot access " + key + " outside of a scoping block");
		}
		return actualValues;
	}
}
