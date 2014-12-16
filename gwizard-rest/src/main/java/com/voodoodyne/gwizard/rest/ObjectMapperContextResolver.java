package com.voodoodyne.gwizard.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

/**
 * A little wiring that lets us give the guice ObjectMapper to JAXRS frameworks like RESTEasy.
 */
@Provider   // Not the guice provider annotation...
public class ObjectMapperContextResolver implements ContextResolver<ObjectMapper> {
    private final ObjectMapper objectMapper;

    @Inject
    public ObjectMapperContextResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return objectMapper;
    }
}
