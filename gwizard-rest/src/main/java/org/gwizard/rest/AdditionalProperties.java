package org.gwizard.rest;

/**
 * If a throwable implements this interface, any extra properties on the object returned
 * from this method will be added to the error JSON. The properties will be "unwrapped",
 * that is, added at the same level as the parent error body object.
 */
public interface AdditionalProperties {
	/**
	 * @return a bean (not a map!) because that's all that jackson @JsonUnwrapped can handle
	 */
	Object getAdditionalProperties();
}
