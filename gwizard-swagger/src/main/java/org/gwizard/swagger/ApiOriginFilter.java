package org.gwizard.swagger;
import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Singleton;

/**
 * Allow CORS --> Swagger specification api.
 */
@Singleton
final class ApiOriginFilter implements Filter {

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
    HttpServletResponse r = (HttpServletResponse) res;
    r.addHeader("Access-Control-Allow-Origin", "*");
    r.addHeader("Access-Control-Allow-Headers", "Content-Type");
    r.addHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT");
    chain.doFilter(req, res);
  }

  @Override
  public void destroy() {}

  @Override
  public void init(FilterConfig fc) throws ServletException {}
  
}