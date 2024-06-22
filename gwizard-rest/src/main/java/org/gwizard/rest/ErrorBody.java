package org.gwizard.rest;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * JSON structure of an error body from the ThrowableMapper. See that class for documentation.
 */
@RequiredArgsConstructor
public class ErrorBody {
	private static String stripExceptionSuffix(final String className) {
		if (className.endsWith("Exception")) {
			return className.substring(0, className.length() - "Exception".length());
		} else {
			return className;
		}
	}

	private final Throwable t;

	public String getMessage() {
		return t.getMessage();
	}

	public String getType() {
		return stripExceptionSuffix(t.getClass().getSimpleName());
	}

	public List<String> getTypes() {
		return Types.getTypes(t, ClientException.class, RuntimeException.class, Exception.class, Throwable.class)
				.stream()
				.map(ErrorBody::stripExceptionSuffix).collect(Collectors.toList());
	}

	public ErrorBody getCause() {
		return t.getCause() == null ? null : new ErrorBody(t.getCause());
	}

	@JsonUnwrapped
	public Object getAdditionalProperties() {
		if (t instanceof AdditionalProperties) {
			return ((AdditionalProperties)t).getAdditionalProperties();
		} else {
			return null;
		}
	}
}
