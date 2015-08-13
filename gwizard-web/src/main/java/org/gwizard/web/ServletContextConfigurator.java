package org.gwizard.web;

import org.eclipse.jetty.servlet.ServletContextHandler;

/**
 * Created by mbessler on 6/30/15.
 */
public interface ServletContextConfigurator {
	void configure(ServletContextHandler sch);
}
