package org.gwizard.test.web;

import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import org.gwizard.test.GuiceExtension;
import org.gwizard.test.Requestor;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 */
public class GuiceWebExtension implements BeforeEachCallback {
	private static final Namespace NAMESPACE = Namespace.create(GuiceWebExtension.class);

	@Override
	public void beforeEach(final ExtensionContext context) throws Exception {
		final Injector injector = GuiceExtension.getInjector(context);

		// This enables the request scope to work
		final GuiceFilter guiceFilter = injector.getInstance(GuiceFilter.class);
		final ServletFilterAdapter guiceFilterAdapter = new ServletFilterAdapter(guiceFilter, MockHttpServletRequest::new, MockHttpServletResponse::new);
		injector.getInstance(Requestor.class).addFilter(guiceFilterAdapter);
	}
}
