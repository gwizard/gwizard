package com.voodoodyne.gwizard.hibernate;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.persistence.EntityManager;

/**
 * <p>Because DI is not always the right idea.</p>
 *
 * <p>Conventional wisdom is to inject the EntityManager into classes where you might need it. This
 * breaks down when you need to perform database access in places where injection is not available.
 * For example, you may have a polymorphic hierarchy of entity objects that exhibit different data-access
 * behavior.</p>
 *
 * <p>Aspects like persistence (aka transaction) and authorization really fit a thread-local model better.
 * This static accessor for the EntityManager gives you "always-available" access to the context.</p>
 */
public class EM {
	@Inject
	static Provider<EntityManager> entityManagerProvider;

	public static EntityManager em() {
		return entityManagerProvider.get();
	}
}
