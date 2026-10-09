package com.google.inject.persist.jpa;

import com.google.inject.persist.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.RollbackException;
import org.aopalliance.intercept.MethodInvocation;
import org.gwizard.hibernate.TransactionRetryConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class RetryingTransactionInterceptorTest {

	private JpaPersistService persistence;
	private EntityManagerFactory factory;
	private EntityManager first;
	private EntityManager second;
	private EntityTransaction firstTransaction;
	private EntityTransaction secondTransaction;
	private MethodInvocation invocation;
	private RetryingTransactionInterceptor.Sleeper sleeper;
	private TransactionRetryConfig config;

	public static class Work {
		@Transactional
		public void run() {}
	}

	@BeforeEach
	void setUp() throws Throwable {
		factory = mock(EntityManagerFactory.class);
		first = mock(EntityManager.class);
		second = mock(EntityManager.class);
		firstTransaction = transaction(first);
		secondTransaction = transaction(second);
		when(factory.createEntityManager()).thenReturn(first, second);
		persistence = new JpaPersistService(JpaPersistOptions.builder().build(), "test", Map.of());
		persistence.start(factory);
		invocation = mock(MethodInvocation.class);
		when(invocation.getMethod()).thenReturn(Work.class.getMethod("run"));
		when(invocation.proceed()).thenReturn("success");
		sleeper = mock(RetryingTransactionInterceptor.Sleeper.class);
		config = new TransactionRetryConfig();
	}

	@AfterEach
	void tearDown() {
		Thread.interrupted();
		persistence.end();
	}

	private static EntityTransaction transaction(final EntityManager manager) {
		final EntityTransaction transaction = mock(EntityTransaction.class);
		final AtomicBoolean active = new AtomicBoolean();
		when(manager.getTransaction()).thenReturn(transaction);
		when(transaction.isActive()).thenAnswer(call -> active.get());
		doAnswer(call -> { active.set(true); return null; }).when(transaction).begin();
		doAnswer(call -> { active.set(false); return null; }).when(transaction).commit();
		doAnswer(call -> { active.set(false); return null; }).when(transaction).rollback();
		return transaction;
	}

	private RetryingTransactionInterceptor interceptor() {
		return new RetryingTransactionInterceptor(persistence, config, sleeper);
	}

	private static PersistenceException serializationFailure() {
		return new PersistenceException(new SQLException("retry", "40001"));
	}

	@Test
	void commitFailureReplaysMethodWithFreshTransactionAndCleansUpBeforeSleeping() throws Throwable {
		doThrow(new RollbackException(serializationFailure())).when(firstTransaction).commit();
		doAnswer(call -> {
			assertThat(persistence.isWorking()).isFalse();
			verify(firstTransaction).rollback();
			verify(first).close();
			return null;
		}).when(sleeper).sleep(anyLong());

		assertThat(interceptor().invoke(invocation)).isEqualTo("success");
		verify(invocation, times(2)).proceed();
		verify(factory, times(2)).createEntityManager();
		verify(secondTransaction).begin();
		verify(secondTransaction).commit();
		verify(second).close();
		assertThat(persistence.isWorking()).isFalse();
	}

	@Test
	void exhaustedRequestScopeGetsCleanContextAndPropagatesLastFailure() throws Throwable {
		persistence.begin();
		config.setMaxAttempts(1);
		final PersistenceException failure = serializationFailure();
		when(invocation.proceed()).thenThrow(failure);
		assertThatThrownBy(() -> interceptor().invoke(invocation)).isSameAs(failure);
		verify(firstTransaction).rollback();
		verify(first).close();
		verifyNoInteractions(sleeper);
		assertThat(persistence.get()).isSameAs(second);
		verify(second, never()).close();
	}

	@Test
	void doesNotRetryCloseFailureAfterSuccessfulCommit() {
		final PersistenceException failure = serializationFailure();
		doThrow(failure).when(first).close();
		assertThatThrownBy(() -> interceptor().invoke(invocation)).isSameAs(failure);
		verify(firstTransaction).commit();
		verify(factory).createEntityManager();
		verifyNoInteractions(sleeper);
		assertThat(persistence.isWorking()).isFalse();
	}

	@Test
	void rollbackFailurePreservesOriginalFailureAndStillClosesContext() throws Throwable {
		final PersistenceException failure = serializationFailure();
		final IllegalStateException cleanup = new IllegalStateException("rollback failed");
		when(invocation.proceed()).thenThrow(failure);
		doThrow(cleanup).when(firstTransaction).rollback();
		assertThatThrownBy(() -> interceptor().invoke(invocation)).isSameAs(failure);
		assertThat(failure.getSuppressed()).containsExactly(cleanup);
		verify(first).close();
		verifyNoInteractions(sleeper);
	}

	@Test
	void interruptionStopsRetriesAndPreservesInterruptStatus() throws Throwable {
		final PersistenceException failure = serializationFailure();
		when(invocation.proceed()).thenThrow(failure);
		doThrow(new InterruptedException("cancelled")).when(sleeper).sleep(anyLong());
		assertThatThrownBy(() -> interceptor().invoke(invocation)).isSameAs(failure);
		assertThat(Thread.currentThread().isInterrupted()).isTrue();
		verify(invocation).proceed();
		verify(first).close();
	}

	@Test
	void alreadyInterruptedThreadDoesNotRetry() throws Throwable {
		when(invocation.proceed()).thenThrow(serializationFailure());
		Thread.currentThread().interrupt();
		assertThatThrownBy(() -> interceptor().invoke(invocation)).isInstanceOf(PersistenceException.class);
		verify(invocation).proceed();
		verifyNoInteractions(sleeper);
	}

	@Test
	void errorsRollBackWithoutRetrying() throws Throwable {
		final AssertionError failure = new AssertionError(serializationFailure());
		when(invocation.proceed()).thenThrow(failure);
		assertThatThrownBy(() -> interceptor().invoke(invocation)).isSameAs(failure);
		verify(firstTransaction).rollback();
		verify(first).close();
		verifyNoInteractions(sleeper);
	}

	@Test
	void manuallyStartedTransactionRemainsOwnedByCaller() throws Throwable {
		persistence.begin();
		firstTransaction.begin();
		when(invocation.proceed()).thenThrow(serializationFailure());
		assertThatThrownBy(() -> interceptor().invoke(invocation)).isInstanceOf(PersistenceException.class);
		verify(invocation).proceed();
		verify(firstTransaction, never()).rollback();
		verify(first, never()).close();
		verifyNoInteractions(sleeper);
	}
}
