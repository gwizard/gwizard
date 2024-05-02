package org.gwizard.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Guice AOP annotation which indicates method call should be logged.</p>
 * <p>Note that methods with this annotation CANNOT BE PRIVATE.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface LogCall
{
	/**
	 * If false, skip logging the result. Default is true.
	 */
	boolean result() default true;
}
