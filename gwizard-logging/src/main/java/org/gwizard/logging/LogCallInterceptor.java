package org.gwizard.logging;


import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Guice AOP interceptor which looks for {@code @LogCall} annotation on methods to log method calls.</p>
 * 
 * <p>Put this in your Guice module configure():</p>
 * <p>{@code bindInterceptor(Matchers.any(), Matchers.annotatedWith(LogCall.class), new LogCallInterceptor());}
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LogCallInterceptor implements MethodInterceptor {

	@Override
	public Object invoke(MethodInvocation inv) throws Throwable {
		final Object obj = inv.getThis();

		// Guice subclasses the application class to intercept its methods.
		final Logger logger = LoggerFactory.getLogger(obj.getClass().getSuperclass());
		if (!logger.isDebugEnabled())
			return inv.proceed();

		final LogCall logCallAnnotation = inv.getMethod().getAnnotation(LogCall.class);

		final StringBuilder msg = new StringBuilder();
		
		msg.append(inv.getMethod().getName());
		msg.append('(');
		
		for (int i=0; i<inv.getArguments().length; i++) {
			final Object arg = inv.getArguments()[i];
			if (arg instanceof String)
				msg.append('"').append(arg).append('"');
			else
				msg.append(arg);
			
			if (i != inv.getArguments().length-1)
				msg.append(", ");
		}

		msg.append(')');
				
		logger.debug("Calling {}", msg);

		try {
			final Object result = inv.proceed();

			if (logCallAnnotation == null || logCallAnnotation.result())
				logger.debug("Returning {}", result);

			return result;
		} catch (Throwable t) {
			logger.debug("Threw " + t);
			throw t;
		}
	}
}
