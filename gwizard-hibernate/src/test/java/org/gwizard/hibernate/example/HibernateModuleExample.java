package org.gwizard.hibernate.example;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.persist.Transactional;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.gwizard.hibernate.DatabaseConfig;
import org.gwizard.hibernate.HibernateModule;

import jakarta.inject.Singleton;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import java.util.UUID;

import static org.gwizard.hibernate.EM.em;
import static org.hibernate.cfg.AvailableSettings.DIALECT;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO;

/**
 * Self-contained example of using Hibernate
 */
public class HibernateModuleExample {
	@Entity(name="Thing")
	@Data
	@NoArgsConstructor
	public static class Thing {
		private @Id UUID id;
		private String name;

		public Thing(String name) {
			setId(UUID.randomUUID());
			setName(name);
		}
	}

	public static class Work {
		@Transactional
		public void makeAThing() {
			em().persist(new Thing("Thing " + Math.random()));
		}

		@Transactional
		public long countThings() {
			final CriteriaBuilder cb = em().getCriteriaBuilder();
			final CriteriaQuery<Long> cq = cb.createQuery(Long.class);
			cq.select(cb.count(cq.from(Thing.class)));
			return em().createQuery(cq).getSingleResult();
		}
	}

	public static class MyModule extends AbstractModule {
		@Override
		protected void configure() {
		}

		@Provides
		@Singleton
		public DatabaseConfig databaseConfig() {
			final DatabaseConfig cfg = new DatabaseConfig();
			cfg.setDriverClass("org.h2.Driver");
			cfg.setUser("sa");
			cfg.setUrl("jdbc:h2:mem:test");
			cfg.getProperties().put(DIALECT, "org.hibernate.dialect.H2Dialect");
			cfg.getProperties().put(HBM2DDL_AUTO, "create");
			return cfg;
		}
	}

	public static void main(String[] args) throws Exception {
		Injector injector = Guice.createInjector(new MyModule(), new HibernateModule());

		Work work = injector.getInstance(Work.class);

		work.makeAThing();
		work.makeAThing();
		work.makeAThing();

		System.out.println("There are now " + work.countThings() + " things");

		// Without this, the program will not exit
		injector.getInstance(EntityManagerFactory.class).close();
	}
}
