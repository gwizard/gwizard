package org.gwizard.config;

import jakarta.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Qualifier for the configFile File
 */
@Qualifier
@Target({FIELD, PARAMETER, METHOD})
@Retention(RUNTIME)
public @interface ConfigFile {
}
