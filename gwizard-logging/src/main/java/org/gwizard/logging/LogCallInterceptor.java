package org.gwizard.logging;


import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;

/**
 * <p>Guice AOP interceptor which looks for {@code @LogCall} annotation on methods to log method calls.</p>
 * 
 * <p>Put this in your Guice module configure():</p>
 * <p>{@code bindInterceptor(Matchers.any(), Matchers.annotatedWith(LogCall.class), new LoggingInterceptor());}
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Slf4j
public class LogCallInterceptor implements MethodInterceptor
{
	/* (non-Javadoc)
	 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
	 */
	@Override
	public Object invoke(MethodInvocation inv) throws Throwable
	{
		final LogCall logCallAnnotation = inv.getMethod().getAnnotation(LogCall.class);

		final Object obj = inv.getThis();

		// The actual class will be the guice enhanced one, so get super
		final Class<?> clazz = obj.getClass().getSuperclass();

		// Turns out GAE always overrides logger value, so no point in doing the lookup
		//final Logger logger = LoggerFactory.getLogger(clazz);
		final Logger logger = log;

		final StringBuilder msg = new StringBuilder();
		
		msg.append(clazz.getSimpleName());
		msg.append('.');
		msg.append(inv.getMethod().getName());
		msg.append('(');
		
		for (int i=0; i<inv.getArguments().length; i++)
		{
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
