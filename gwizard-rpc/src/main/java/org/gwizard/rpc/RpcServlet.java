package org.gwizard.rpc;

import com.google.inject.Injector;
import com.voodoodyne.trivet.TrivetServlet;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * The glue that turns an RPC request into a method call
 */
@Singleton
public class RpcServlet extends TrivetServlet {

	private final Injector injector;

	@Inject
	public RpcServlet(Injector injector) {
		this.injector = injector;
	}

	@Override
	public Object getInstance(Class<?> aClass) {
		return injector.getInstance(aClass);
	}
}
