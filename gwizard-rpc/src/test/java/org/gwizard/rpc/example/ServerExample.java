package org.gwizard.rpc.example;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import org.gwizard.rpc.RpcModule;
import org.gwizard.services.Run;

/**
 * Example of an RPC server
 */
public class ServerExample {

	public static class MyModule extends AbstractModule {
		@Override
		protected void configure() {
			bind(Hello.class).to(HelloImpl.class);
		}
	}

	public static void main(String[] args) throws Exception {
		Guice.createInjector(new MyModule(), new RpcModule()).getInstance(Run.class).start();
	}
}
