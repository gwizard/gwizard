package org.gwizard.rpc.example;

import com.voodoodyne.trivet.Client;

/**
 * Simple client to the rpc server
 */
public class ClientExample {

	public static void main(String[] args) throws Exception {
		String name = args.length >= 1 ? args[0] : "world";

		Hello hello = Client.create("http://localhost:8080/rpc", Hello.class);
		String result = hello.hi(name);

		System.out.println(result);
	}
}
