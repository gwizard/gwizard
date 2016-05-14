package org.gwizard.hibernate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.AvailableSettings.DIALECT;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO;

import java.util.Map;

import javax.persistence.EntityManagerFactory;

import org.gwizard.hibernate.example.HibernateModuleExample.MyModule;
import org.gwizard.hibernate.example.HibernateModuleExample.Work;
import org.testng.annotations.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class HibernateModuleTest {

	@Test
	public void bridgeModuleOverridesJpaProperties() {
		// given
		MyModule myModule = new MyModule();
		Map<String, String> configProperties = myModule.databaseConfig().getProperties();
		Injector injector = Guice.createInjector(myModule, new HibernateModule());

		// when
		EntityManagerFactory entityManagerFactory = injector.getInstance(EntityManagerFactory.class);

		Map<String, Object> actual = entityManagerFactory.getProperties();
		assertThat(actual.get(DIALECT)).isEqualTo(configProperties.get(DIALECT));
		assertThat(actual.get(HBM2DDL_AUTO)).isEqualTo(configProperties.get(HBM2DDL_AUTO));

		entityManagerFactory.close();
	}

	@Test
	public void canPersistAndQueryEntityManager() {
		// given
		MyModule myModule = new MyModule();
		Injector injector = Guice.createInjector(myModule, new HibernateModule());
		Work work = injector.getInstance(Work.class);

		// when
		assert(work.countThings()) == 0;
		work.makeAThing();
		work.makeAThing();
		work.makeAThing();

		// then
		assert(work.countThings()) == 3;
		injector.getInstance(EntityManagerFactory.class).close();
	}
}