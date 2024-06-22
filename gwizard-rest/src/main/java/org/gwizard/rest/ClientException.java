package org.gwizard.rest;

import lombok.Getter;

import jakarta.ws.rs.core.Response.Status;

/**
 * Extend this to produce a 400-level error code to the client instead of a 500-level error.
 * The difference between this exception hierarchy and the WebApplicationException hierarchy
 * (ClientErrorException etc) is that those will render the Response as contained in the exception;
 * this (and any other random exception) gets rendered by the ThrowableMapper as JSON.
 */
public class ClientException extends RuntimeException {
	@Getter
	private final Status status;

	public ClientException() {
		this(Status.BAD_REQUEST);
	}

	public ClientException(final String message) {
		this(message, Status.BAD_REQUEST);
	}

	public ClientException(final String message, final Throwable cause) {
		this(message, cause, Status.BAD_REQUEST);
	}

	public ClientException(final Throwable cause) {
		this(cause, Status.BAD_REQUEST);
	}

	public ClientException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
		this(message, cause, enableSuppression, writableStackTrace, Status.BAD_REQUEST);
	}

	public ClientException(final Status status) {
		this.status = status;
	}

	public ClientException(final String message, final Status status) {
		super(message);
		this.status = status;
	}

	public ClientException(final String message, final Throwable cause, final Status status) {
		super(message, cause);
		this.status = status;
	}

	public ClientException(final Throwable cause, final Status status) {
		super(cause);
		this.status = status;
	}

	public ClientException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace, final Status status) {
		super(message, cause, enableSuppression, writableStackTrace);
		this.status = status;
	}
}
