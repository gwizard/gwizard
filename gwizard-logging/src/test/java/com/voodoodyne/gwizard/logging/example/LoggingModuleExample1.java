package com.voodoodyne.gwizard.logging.example;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.voodoodyne.gwizard.logging.LoggingModule;
import lombok.extern.slf4j.Slf4j;

/**
 * This uses logback's default configuration bootstrap routine, looking for logback.xml etc.
 */
@Slf4j
public class LoggingModuleExample1 {
	public static void main(String[] args) throws Exception {
		final Injector injector = Guice.createInjector(new LoggingModule());

		log.debug("Hello, logback");
	}
}
