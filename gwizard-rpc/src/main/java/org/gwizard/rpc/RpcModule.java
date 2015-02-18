package org.gwizard.rpc;

import com.google.inject.servlet.ServletModule;
import lombok.EqualsAndHashCode;
import org.gwizard.web.WebModule;

/**
 * Sets up an RPC listener.
 */
@EqualsAndHashCode(callSuper=false)	// makes installation of this module idempotent per endpoint
public class RpcModule extends ServletModule {

	/** Endpoint path for rpc requests, eg /rpc */
	private final String endpoint;

	/** Default endpoint path is /rpc */
	public RpcModule() {
		this("/rpc");
	}

	/**
	 * @param endpoint is the path at which rpc requests are listened for
	 */
	public RpcModule(String endpoint) {
		this.endpoint = endpoint.startsWith("/") ? endpoint : "/" + endpoint;
	}

	@Override
	protected void configureServlets() {
		install(new WebModule());

		serve(endpoint).with(RpcServlet.class);
	}
}
