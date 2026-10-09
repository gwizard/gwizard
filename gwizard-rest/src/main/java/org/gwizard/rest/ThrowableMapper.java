package org.gwizard.rest;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.spi.Failure;

/**
 * <p>This is an optional error handling mechanism that you may enable by registering it with Guice.
 * It exposes - to the world - a json structure that includes exception types and messages,
 * understandable by programs and humans alike.</p>
 *
 * <p>This is controversial. If your app is public-facing, exception messages may disclose information that
 * you do not intend. On the other hand, users may give you valuable feedback beyond "it's broken". And
 * not every application is security-sensitive or public-facing. Use your discretion.</p>
 *
 * <p>Leaves alone the exception responses that are internal to JAXRS.</p>
 */
@Provider
@Singleton
@Slf4j
public class ThrowableMapper implements ExceptionMapper<Throwable> {
	@Override
	public Response toResponse(Throwable exception) {
		if (exception instanceof WebApplicationException) {
			return ((WebApplicationException)exception).getResponse();
		} else if (exception instanceof Failure failure) {
			final Response response = failure.getResponse();
			if (response != null)
				return response;

			// Failure may contain only a status code. Returning null would turn it into HTTP 204.
			final int status = failure.getErrorCode();
			// Validate explicitly: RESTEasy treats -1 as an unspecified status and would also produce 204.
			if (status < 100 || status > 599)
				throw new IllegalArgumentException("Invalid HTTP status code in RESTEasy Failure: " + status, failure);
			return Response.status(status).build();
		} else {
			log.error("Exception hit the top level handler", exception);

			final Status status = (exception instanceof ClientException)
					? ((ClientException)exception).getStatus()
					: Status.INTERNAL_SERVER_ERROR;

			return Response
					.status(status)
					.type(MediaType.APPLICATION_JSON_TYPE)
					.entity(new ErrorBody(exception))
					.build();
		}
	}
}
