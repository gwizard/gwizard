package org.gwizard.test.hibernate;

import com.google.inject.Injector;
import com.google.inject.persist.PersistFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.gwizard.test.GuiceExtension;
import org.gwizard.test.Requestor;
import org.gwizard.test.web.ServletFilterAdapter;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Optional; if using guice-persist, this runs the PersistFilter around every req() call.
 */
public class GuicePersistExtension implements BeforeEachCallback, AfterEachCallback {

	@Override
	public void beforeEach(final ExtensionContext context) throws Exception {
		final Injector injector = GuiceExtension.getInjector(context);

		final PersistFilter filter = injector.getInstance(PersistFilter.class);

		final ServletFilterAdapter adapter = new ServletFilterAdapter(filter, injector.getProvider(HttpServletRequest.class), injector.getProvider(HttpServletResponse.class));
		injector.getInstance(Requestor.class).addFilter(adapter);

		filter.init(null);
	}

	@Override
	public void afterEach(final ExtensionContext context) throws Exception {
		final Injector injector = GuiceExtension.getInjector(context);

		final PersistFilter filter = injector.getInstance(PersistFilter.class);

		filter.destroy();
	}

}
