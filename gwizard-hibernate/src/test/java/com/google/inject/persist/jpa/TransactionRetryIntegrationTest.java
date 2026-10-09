package com.google.inject.persist.jpa;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistFilter;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.Transactional;
import com.google.inject.util.Modules;
import io.dropwizard.util.Duration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import org.gwizard.hibernate.DatabaseConfig;
import org.gwizard.hibernate.DatabaseConfigProperties;
import org.gwizard.hibernate.HibernateModule;
import org.gwizard.hibernate.example.HibernateModuleExample.Thing;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.gwizard.hibernate.EM.em;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class TransactionRetryIntegrationTest {

	private Injector injector;
	private Work work;
	private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

	@BeforeEach
	void setUp() {
		final DatabaseConfigProperties config = new DatabaseConfigProperties(
				"org.h2.Driver", "jdbc:h2:mem:" + UUID.randomUUID(), "sa", "");
		config.getProperties().put(HBM2DDL_AUTO, "create-drop");
		config.getTransactionRetry().setMaxAttempts(3);
		config.getTransactionRetry().setMaxBackoff(Duration.milliseconds(1));
		config.getProperties().put("hibernate.connection.isolation", "8"); // SERIALIZABLE
		injector = Guice.createInjector(new HibernateModule(), binder -> {
			binder.bind(DatabaseConfig.class).toInstance(config);
			binder.bind(MeterRegistry.class).toInstance(registry);
		});
		work = injector.getInstance(Work.class);
	}

	@AfterEach
	void tearDown() {
		injector.getInstance(JpaPersistService.class).end();
		injector.getInstance(PersistService.class).stop();
		registry.close();
	}

	@Test
	void methodFailuresRollBackWritesAndReplaceEntityManager() {
		work.create(2);
		assertMetrics(3, 2, 2, 0);
		assertThat(work.attempts).hasSize(3).doesNotHaveDuplicates();
		assertThat(work.attempts).allMatch(manager -> !manager.isOpen());
		assertThat(work.count()).isEqualTo(1);
		assertThat(injector.getInstance(JpaPersistService.class).isWorking()).isFalse();
	}

	@Test
	void nestedCallsRetryTheWholeOuterTransaction() {
		final OuterWork outer = injector.getInstance(OuterWork.class);
		outer.inner = work;
		outer.create();
		assertMetrics(3, 2, 2, 0);
		assertThat(outer.attempts).containsExactlyElementsOf(work.attempts).hasSize(3);
		assertThat(work.count()).isEqualTo(2); // Both outer and inner writes from just the successful attempt.
	}

	@Test
	void persistFilterKeepsSuccessfulContextOpenUntilRequestEnds() throws Exception {
		final PersistFilter filter = injector.getInstance(PersistFilter.class);
		final List<EntityManager> requestContexts = new ArrayList<>();
		filter.doFilter(null, null, (request, response) -> {
			final EntityManager original = em();
			work.create(1);
			assertThat(original.isOpen()).isFalse();
			final EntityManager successful = em();
			requestContexts.add(successful);
			assertThat(successful).isSameAs(work.attempts.get(1));
			assertThat(successful.isOpen()).isTrue();
			assertThat(work.count()).isEqualTo(1);
			assertThat(em()).isSameAs(successful);
		});
		assertThat(requestContexts).allMatch(manager -> !manager.isOpen());
		assertThat(injector.getInstance(JpaPersistService.class).isWorking()).isFalse();
	}

	@Test
	void exhaustionUsesConfiguredAttemptCountAndLeavesNoWrites() {
		final Throwable failure = catchThrowable(() -> work.create(10));
		assertMetrics(3, 2, 3, 1);
		assertThat(work.attempts).hasSize(3);
		assertThat(failure).isSameAs(work.failures.get(2));
		assertThat(work.count()).isZero();
		assertThat(injector.getInstance(JpaPersistService.class).isWorking()).isFalse();
	}

	@Test
	void ordinaryFailuresAreNotRetriedAndRollbackMetadataStillWorks() throws Exception {
		assertThatThrownBy(work::ordinaryFailure).isInstanceOf(IllegalArgumentException.class);
		assertMetrics(1, 0, 0, 0);
		assertThat(work.attempts).hasSize(1);
		assertThat(work.count()).isZero();

		final MetadataWork metadata = injector.getInstance(MetadataWork.class);
		assertThatThrownBy(metadata::checkedRollback).isInstanceOf(Exception.class);
		assertThat(work.count()).isZero();
		assertThatThrownBy(metadata::checkedCommit).isInstanceOf(Exception.class);
		assertThat(work.count()).isEqualTo(1);
		assertThatThrownBy(metadata::ignoredCommit).isInstanceOf(IllegalArgumentException.class);
		assertThat(work.count()).isEqualTo(2);
	}

	@Test
	void classAndMethodAnnotationsDoNotMultiplyRetryBudget() {
		final MetadataWork metadata = injector.getInstance(MetadataWork.class);
		assertThatThrownBy(metadata::serializationFailureDespiteIgnore).isInstanceOf(PersistenceException.class);
		assertThat(metadata.attempts).isEqualTo(3);
		assertThat(work.count()).isZero();
	}

	@Test
	void actualDatabaseSerializationConflictReloadsData() throws Exception {
		final UUID id = work.create(0);
		final ConflictingWork conflicting = injector.getInstance(ConflictingWork.class);
		injector.getInstance(PersistFilter.class).doFilter(null, null, (request, response) -> {
			final EntityManager original = em();
			conflicting.update(id);
			assertThat(original.isOpen()).isFalse();
			assertThat(em().isOpen()).isTrue();
			assertThat(em().find(Thing.class, id).getName()).isEqualTo("concurrent updated");
		});
		assertThat(conflicting.observedNames).containsExactly("attempt", "concurrent");
		assertThat(work.name(id)).isEqualTo("concurrent updated");
	}

	@Test
	void invalidConfigurationFailsAtInjectorCreation() {
		final DatabaseConfigProperties config = new DatabaseConfigProperties();
		config.getTransactionRetry().setMaxAttempts(0);
		final PersistService service = mock(PersistService.class);
		assertThatThrownBy(() -> Guice.createInjector(
				Modules.override(new HibernateModule()).with(binder -> {
					binder.bind(DatabaseConfig.class).toInstance(config);
					binder.bind(PersistService.class).toInstance(service);
				}))).hasMessageContaining("transactionRetry.maxAttempts must be at least 1");
		verifyNoInteractions(service);
	}

	private void assertMetrics(final double attempts, final double retries, final double failures, final double exhausted) {
		assertThat(registry.get("db.transaction.attempts").tag("persistence.unit", "gw-persistence-unit").counter().count()).isEqualTo(attempts);
		assertThat(registry.get("db.transaction.retries").counter().count()).isEqualTo(retries);
		assertThat(registry.get("db.transaction.serialization.failures").counter().count()).isEqualTo(failures);
		assertThat(registry.get("db.transaction.retry.exhausted").counter().count()).isEqualTo(exhausted);
	}

	private static PersistenceException serializationFailure() {
		return new PersistenceException(new SQLException("serialization conflict", "40001"));
	}

	public static class Work {
		final List<EntityManager> attempts = new ArrayList<>();
		final List<PersistenceException> failures = new ArrayList<>();
		@Inject Provider<EntityManager> managers;

		@Transactional
		public UUID create(final int failuresBeforeSuccess) {
			assertThat(managers.get()).isSameAs(em());
			attempts.add(em());
			final Thing thing = new Thing("attempt");
			em().persist(thing);
			em().flush();
			if (attempts.size() <= failuresBeforeSuccess) {
				final PersistenceException failure = serializationFailure();
				failures.add(failure);
				throw failure;
			}
			return thing.getId();
		}

		@Transactional
		public void ordinaryFailure() {
			attempts.add(em());
			em().persist(new Thing("rollback"));
			em().flush();
			throw new IllegalArgumentException("application failure");
		}

		@Transactional
		public long count() {
			return em().createQuery("select count(t) from Thing t", Long.class).getSingleResult();
		}

		@Transactional
		public String name(final UUID id) {
			return em().find(Thing.class, id).getName();
		}
	}

	public static class OuterWork {
		final List<EntityManager> attempts = new ArrayList<>();
		Work inner;

		@Transactional
		public void create() {
			attempts.add(em());
			em().persist(new Thing("outer"));
			inner.create(2);
		}
	}

	@Transactional(rollbackOn = Exception.class)
	public static class MetadataWork {
		int attempts;

		public void checkedRollback() throws Exception {
			em().persist(new Thing("checked rollback"));
			throw new Exception("checked");
		}

		@Transactional
		public void checkedCommit() throws Exception {
			em().persist(new Thing("checked commit"));
			throw new Exception("checked");
		}

		@Transactional(ignore = IllegalArgumentException.class)
		public void ignoredCommit() {
			em().persist(new Thing("ignored commit"));
			throw new IllegalArgumentException("ignored");
		}

		@Transactional(ignore = PersistenceException.class)
		public void serializationFailureDespiteIgnore() {
			attempts++;
			em().persist(new Thing("always rollback"));
			em().flush();
			throw serializationFailure();
		}
	}

	public static class ConflictingWork {
		@Inject EntityManagerFactory factory;
		final List<String> observedNames = new ArrayList<>();

		@Transactional
		public void update(final UUID id) {
			final Thing thing = em().find(Thing.class, id);
			observedNames.add(thing.getName());
			if (observedNames.size() == 1) {
				// Change the row after this transaction's snapshot was established.
				try (EntityManager concurrent = factory.createEntityManager()) {
					concurrent.getTransaction().begin();
					concurrent.find(Thing.class, id).setName("concurrent");
					concurrent.getTransaction().commit();
				}
			}
			thing.setName(thing.getName() + " updated");
			// Hibernate flushes the conflicting update during commit, outside this method.
		}
	}
}
