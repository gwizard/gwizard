/*
 */

package org.gwizard.test.web;

import com.google.inject.servlet.RequestScoped;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * HttpServletRequest which implements only the attribute-related methods (and not even
 * all of those).  Suitable as a scope for framework bits that need such a thing.
 *
 * All methods except attribute-related methods will either do nothing or return null.
 */
@RequestScoped
public class FakeHttpServletRequest extends HttpServletRequestWrapper {

	/** Create a stub interface via dynamic proxy that does nothing */
	private static HttpServletRequest makeStub() {
		return (HttpServletRequest)Proxy.newProxyInstance(
				Thread.currentThread().getContextClassLoader(),
				new Class<?>[] { HttpServletRequest.class },
				(proxy, method, args) -> null);
	}

	final Map<String, Object> attrs = new HashMap<>();

	public FakeHttpServletRequest() {
		super(makeStub());
	}

	@Override
	public Object getAttribute(String key) {
		return attrs.get(key);
	}

	@Override
	public void setAttribute(String key, Object value) {
		attrs.put(key, value);
	}

	@Override
	public void removeAttribute(String key) {
		attrs.remove(key);
	}

	@Override
	public String getRemoteAddr() {
		return "127.0.0.1";
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		return Collections.emptyEnumeration();
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		return Collections.emptyMap();
	}
}
