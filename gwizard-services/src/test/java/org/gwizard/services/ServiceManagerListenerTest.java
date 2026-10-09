package org.gwizard.services;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceManagerListenerTest {
	@Test
	void processExitDoesNotBlockLifecycleCallbacksAndIsRequestedOnlyOnce() throws Exception {
		final CountDownLatch exiting = new CountDownLatch(1);
		final CountDownLatch shutdownHook = new CountDownLatch(1);
		final AtomicInteger exitCalls = new AtomicInteger();
		final AtomicInteger status = new AtomicInteger();
		final AtomicReference<Thread> exitThread = new AtomicReference<>();
		final ServiceManagerListener listener = new ServiceManagerListener(code -> {
			exitThread.set(Thread.currentThread());
			exitCalls.incrementAndGet();
			status.set(code);
			exiting.countDown();
			try {
				shutdownHook.await(5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		final PhasedServiceTest.ControlledService service = new PhasedServiceTest.ControlledService();
		service.startAsync();
		service.fail(new RuntimeException("failed"));
		try {
			listener.failure(service);
			assertThat(exiting.await(5, TimeUnit.SECONDS)).isTrue();
			listener.failure(service);
			assertThat(exitCalls.get()).isEqualTo(1);
			assertThat(status.get()).isEqualTo(1);
			assertThat(exitThread.get()).isNotSameAs(Thread.currentThread());
		} finally {
			shutdownHook.countDown();
		}
	}
}
