package org.gwizard.rpc;

import com.google.inject.Injector;
import com.voodoodyne.trivet.TrivetServlet;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.Serial;

/**
 * The glue that turns an RPC request into a method call
 */
@Singleton
public class RpcServlet extends TrivetServlet {

	@Serial
	private static final long serialVersionUID = 1L;

	@Inject
	public RpcServlet(final Injector injector) {
		super(injector::getInstance);
	}

}
