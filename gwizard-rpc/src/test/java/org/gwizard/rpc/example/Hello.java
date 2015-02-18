package org.gwizard.rpc.example;

/**
 * Just a plain old interface. If you don't want to have to bind your interfaces to implementations
 * explicitly in your module, you can add @ImplementedBy(HelloImpl.class) here.
 */
public interface Hello {
	String hi(String name);
}
