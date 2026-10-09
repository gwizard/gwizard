package org.gwizard.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.core.providerfactory.ResteasyProviderFactoryImpl;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.Failure;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ThrowableMapperTest {

	private final ThrowableMapper mapper = new ThrowableMapper();

	@Test
	void statusOnlyFailuresKeepTheirStatus() {
		for (int status : new int[] {400, 499, 503, 599}) {
			try (Response response = mapper.toResponse(new Failure(status))) {
				assertThat(response).isNotNull();
				assertThat(response.getStatus()).isEqualTo(status);
			}
		}
	}

	@Test
	void missingOrInvalidFailureStatusIsReportedWithTheOriginalCause() {
		for (int status : new int[] {-1, 0, 99, 600}) {
			final Failure failure = new Failure(status);
			assertThatThrownBy(() -> mapper.toResponse(failure))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining(Integer.toString(status))
					.hasCause(failure);
		}
	}

	@Test
	void existingFailureResponseIsPreservedIncludingHeadersAndBody() {
		try (Response response = Response.status(429).header("Retry-After", "60").entity("Try again later").build()) {
			assertThat(mapper.toResponse(new Failure("Busy", response))).isSameAs(response);
		}
	}

	@Test
	void existingWebApplicationResponseIsPreservedIncludingHeadersAndBody() {
		try (Response response = Response.status(401).header("WWW-Authenticate", "Bearer").entity("Unauthorized").build()) {
			assertThat(mapper.toResponse(new WebApplicationException(response))).isSameAs(response);
		}
	}

	@Test
	void clientExceptionsKeepTheirJsonErrorResponse() {
		try (Response response = mapper.toResponse(new ClientException("Not allowed", Response.Status.FORBIDDEN))) {
			assertThat(response.getStatus()).isEqualTo(403);
			assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
			assertThat(response.getEntity()).isInstanceOfSatisfying(ErrorBody.class, body -> {
				assertThat(body.getMessage()).isEqualTo("Not allowed");
				assertThat(body.getType()).isEqualTo("Client");
			});
		}
	}

	@Test
	void unexpectedExceptionsKeepTheirInternalServerErrorResponse() {
		try (Response response = mapper.toResponse(new IllegalStateException("Unexpected failure"))) {
			assertThat(response.getStatus()).isEqualTo(500);
			assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
			assertThat(response.getEntity()).isInstanceOfSatisfying(ErrorBody.class, body -> {
				assertThat(body.getMessage()).isEqualTo("Unexpected failure");
				assertThat(body.getType()).isEqualTo("IllegalState");
			});
		}
	}

	@Test
	void resteasyDispatchesStatusOnlyFailureAsAnErrorInsteadOfNoContent() throws Exception {
		final var dispatcher = dispatcher(new Failure(503));
		final var response = new MockHttpResponse();

		dispatcher.invoke(MockHttpRequest.get("/failure"), response);

		assertThat(response.getStatus()).isEqualTo(503);
	}

	@Test
	void resteasyPropagatesInvalidStatusWithTheOriginalCauseInsteadOfProducingASuccessfulResponse() {
		for (int status : new int[] {-1, 600}) {
			final Failure failure = new Failure(status);
			final var dispatcher = dispatcher(failure);
			assertThatThrownBy(() -> dispatcher.invoke(MockHttpRequest.get("/failure"), new MockHttpResponse()))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining(Integer.toString(status))
					.hasCause(failure);
		}
	}

	private SynchronousDispatcher dispatcher(final Failure failure) {
		final var dispatcher = new SynchronousDispatcher(new ResteasyProviderFactoryImpl());
		dispatcher.getProviderFactory().registerProviderInstance(mapper);
		dispatcher.getRegistry().addSingletonResource(new FailureResource(failure));
		return dispatcher;
	}

	@Path("/failure")
	public static class FailureResource {
		private final Failure failure;

		public FailureResource(final Failure failure) {
			this.failure = failure;
		}

		@GET
		public String fail() {
			throw failure;
		}
	}
}
