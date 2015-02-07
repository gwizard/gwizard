package org.gwizard.eventcast;

import com.google.inject.AbstractModule;
import com.lexicalscope.eventcast.EventCast;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@EqualsAndHashCode(callSuper=false, of={})	// makes installation of this module idempotent
public class EventCastModule extends AbstractModule {

	@Override
	protected void configure() {
		install(EventCast.eventCastModuleBuilder().build());
		// leave up to consumer?
		bind(UnhandledEventListener.class).asEagerSingleton();
	}
}
