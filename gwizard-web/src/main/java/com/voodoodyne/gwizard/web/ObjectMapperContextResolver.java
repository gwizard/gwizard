package com.voodoodyne.gwizard.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import javax.ws.rs.ext.ContextResolver;

/**
 * A little wiring that lets us give the guice ObjectMapper to jersey.
 */
class ObjectMapperContextResolver implements ContextResolver<ObjectMapper> {
    private final ObjectMapper objectMapper;

    @Inject
    ObjectMapperContextResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return objectMapper;
    }
}
