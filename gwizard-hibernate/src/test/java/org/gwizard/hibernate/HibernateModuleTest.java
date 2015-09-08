package org.gwizard.hibernate;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.gwizard.hibernate.example.HibernateModuleExample.MyModule;
import org.hamcrest.collection.IsMapContaining;
import org.testng.annotations.Test;

import javax.persistence.EntityManagerFactory;
import java.util.Map;

import static org.gwizard.hibernate.example.HibernateModuleExample.Work;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hibernate.cfg.AvailableSettings.DIALECT;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO;

public class HibernateModuleTest {

	@Test
	public void bridgeModuleOverridesJpaProperties() {
		// given
		MyModule myModule = new MyModule();
		Map<String, String> configProperties = myModule.databaseConfig().getProperties();
		Injector injector = Guice.createInjector(myModule, new HibernateModule());

		// when
		EntityManagerFactory entityManagerFactory = injector.getInstance(EntityManagerFactory.class);

		assertThat(entityManagerFactory.getProperties(), IsMapContaining.hasEntry(
				equalTo(DIALECT), equalTo(configProperties.get(DIALECT))));
		assertThat(entityManagerFactory.getProperties(), IsMapContaining.hasEntry(
				equalTo(HBM2DDL_AUTO), equalTo(configProperties.get(HBM2DDL_AUTO))));

		entityManagerFactory.close();
	}

	@Test
	public void canPersistAndQueryEntityManager() {
		// given
		MyModule myModule = new MyModule();
		Injector injector = Guice.createInjector(myModule, new HibernateModule());

		// when
		Work work = injector.getInstance(Work.class);

		// then
		assertThat(work.countThings(), is(0L));
		work.makeAThing();
		work.makeAThing();
		work.makeAThing();
		assertThat(work.countThings(), is(3L));

		injector.getInstance(EntityManagerFactory.class).close();
	}
}