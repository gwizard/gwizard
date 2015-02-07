

package org.gwizard.eventcast.example;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.lexicalscope.eventcast.EventCast;
import lombok.extern.slf4j.Slf4j;
import org.gwizard.eventcast.EventCastModule;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class EventCastModuleExample {

	/**
	 * a simple event
	 */
	public static interface MyEventListener {
		void eventOccurred(String foo);
	}

	/**
	 * a different event
	 */
	public static interface MyOtherEventListener {
		void eventOccurred(String foo, String bar);
	}

	/**
	 * implementation for a class that receives both sorts of events
	 */
	@Slf4j
	public static class MyEventReceiver implements MyEventListener, MyOtherEventListener  {
		private static final AtomicLong instanceCount = new AtomicLong(0);
		private final long instanceId;

		@Inject
		public MyEventReceiver() {
			instanceId = instanceCount.getAndIncrement();
		}

		@Override
		public void eventOccurred(String foo) {
			log.info("[receiver{}] Event {}", instanceId, foo);

			// sleep to make the handler behave badly
			Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MILLISECONDS);
		}

		@Override
		public void eventOccurred(String foo, String bar) {
			log.info("[receiver{}] OtherEvent {}, {}", instanceId, foo, bar);
		}
	}

	/**
	 * implementation that receives only one sort of event
	 */
	@Slf4j
	public static class MyEventReceiver2 implements MyEventListener {

		@Override
		public void eventOccurred(String foo) {
			log.info("Event {}", foo);
		}
	}

	/**
	 * a class that sends events
	 */
	@Slf4j
	public static class MyEventSender {
		private final MyEventListener eventListener;

		@Inject
		public MyEventSender(MyEventListener eventListener) {
			this.eventListener = eventListener;
		}

		public void trigger(String eventParam) {
			eventListener.eventOccurred(eventParam);
		}
	}

	/**
	 */
	public static class ExampleModule extends AbstractModule {
		@Override
		protected void configure() {
			// EventCastModule has bound the event caster, consumers should
			// use eventCastBindingModuleBuilder()
			install(EventCast.eventCastBindingModuleBuilder()
					.implement(MyEventListener.class, Executors.newFixedThreadPool(2)) // oooh async, fancy shmancy
					.implement(MyOtherEventListener.class)
					.build());

		}
	}

	/**
	 */
	public static void main(String[] args) throws Exception {
		final Injector injector = Guice.createInjector(
				new EventCastModule(),
				new ExampleModule());

		// prior to sending events we need to create receivers
		// (otherwise, you'll see the UnhandledEvent warning from UnhandledEventListener)
		for (int count: ImmutableSet.of(0, 1, 2)) {
			injector.getInstance(MyEventReceiver.class);
		}
		injector.getInstance(MyEventReceiver2.class);

		// receivers created, now we can send events

		// send event using sender impl
		injector.getInstance(MyEventSender.class).trigger("foo");

		injector.getInstance(MyOtherEventListener.class).eventOccurred("bar", "baz");
	}
}