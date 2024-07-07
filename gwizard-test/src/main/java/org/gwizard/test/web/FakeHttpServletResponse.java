/*
 */

package org.gwizard.test.web;

import com.google.inject.servlet.RequestScoped;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.lang.reflect.Proxy;

/**
 * HttpServletResponse which does nothing.
 * 
 * All methods will throw UnsupportedOperationException.
 */
@RequestScoped
public class FakeHttpServletResponse extends HttpServletResponseWrapper {
	/** Create a stub interface via dynamic proxy that does nothing */
	private static HttpServletResponse makeStub() {
		return (HttpServletResponse)Proxy.newProxyInstance(
				Thread.currentThread().getContextClassLoader(),
				new Class<?>[] { HttpServletResponse.class },
				(proxy, method, args) -> {
					throw new UnsupportedOperationException();
				});
	}
	
	public FakeHttpServletResponse() {
		super(makeStub());
	}
}
