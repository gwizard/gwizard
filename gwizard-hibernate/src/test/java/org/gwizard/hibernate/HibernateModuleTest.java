package org.gwizard.hibernate;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import org.gwizard.hibernate.example.HibernateModuleExample.MyModule;
import org.gwizard.hibernate.example.HibernateModuleExample.Work;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManagerFactory;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.AvailableSettings.DIALECT;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO;

public class HibernateModuleTest {

	@Test
	public void bridgeModuleOverridesJpaProperties() {
		// given
		final MyModule myModule = new MyModule();
		final Map<String, String> configProperties = myModule.databaseConfig().getProperties();
		final Injector injector = Guice.createInjector(myModule, new HibernateModule());

		injector.getInstance(PersistService.class).start();

		// when
		final EntityManagerFactory entityManagerFactory = injector.getInstance(EntityManagerFactory.class);

		final Map<String, Object> actual = entityManagerFactory.getProperties();
		assertThat(actual.get(DIALECT)).isEqualTo(configProperties.get(DIALECT));
		assertThat(actual.get(HBM2DDL_AUTO)).isEqualTo(configProperties.get(HBM2DDL_AUTO));

		entityManagerFactory.close();
	}

	@Test
	public void canPersistAndQueryEntityManager() {
		// given
		final MyModule myModule = new MyModule();
		final Injector injector = Guice.createInjector(myModule, new HibernateModule());
		injector.getInstance(PersistService.class).start();

		final Work work = injector.getInstance(Work.class);

		// when
		assertThat(work.countThings()).isZero();
		work.makeAThing();
		work.makeAThing();
		work.makeAThing();

		// then
		assertThat(work.countThings()).isEqualTo(3);
		injector.getInstance(EntityManagerFactory.class).close();
	}
}