

package org.gwizard.eventcast.example;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.lexicalscope.eventcast.EventCast;
import lombok.extern.slf4j.Slf4j;
import org.gwizard.eventcast.EventCastModule;

import java.util.concurrent.Executors;
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
		}

		@Override
		public void eventOccurred(String foo, String bar) {
			log.info("[receiver{}] OtherEvent {}, {}", instanceId, foo, bar);
		}
	}

	@Slf4j
	public static class MyEventReceiver2 implements MyEventListener {

		@Override
		public void eventOccurred(String foo) {
			log.info("Event {}", foo);
		}
	}

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

	@Slf4j
	public static class MyOtherEventSender {
		private final MyOtherEventListener eventListener;

		@Inject
		public MyOtherEventSender(MyOtherEventListener eventListener) {
			this.eventListener = eventListener;
		}

		public void trigger(String eventParam1, String eventParam2) {
			eventListener.eventOccurred(eventParam1, eventParam2);
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
					.implement(MyEventListener.class, Executors.newFixedThreadPool(3)) // demonstrate async
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

		// now, send events
		injector.getInstance(MyEventSender.class).trigger("foo");
		injector.getInstance(MyOtherEventSender.class).trigger("bar", "baz");
	}
}