package org.gwizard.rpc.example;

import com.voodoodyne.trivet.Remote;

/**
 * Implementations must be flagged as @Remote. By default all interfaces can be used remotely, but
 * you can restrict to specific like this: @Remote({Hello.class, Foo.class})
 */
@Remote	// or @Remote(Hello.class) to only allow that interface to be used remotely
public class HelloImpl implements Hello {
	@Override
	public String hi(String name) {
		return "Hello, " + name;
	}
}
