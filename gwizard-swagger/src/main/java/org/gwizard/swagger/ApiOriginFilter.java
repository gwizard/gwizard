package org.gwizard.swagger;

import com.google.inject.Singleton;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Allow CORS --> Swagger specification api.
 */
@Singleton
final class ApiOriginFilter implements Filter {

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		HttpServletResponse r = (HttpServletResponse)res;
		r.addHeader("Access-Control-Allow-Origin", "*");
		r.addHeader("Access-Control-Allow-Headers", "Content-Type");
		r.addHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT");
		chain.doFilter(req, res);
	}

	@Override
	public void destroy() {
	}

	@Override
	public void init(FilterConfig fc) throws ServletException {
	}

}