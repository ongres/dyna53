/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dyna53.jackson;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonFactoryBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;


/** This class is used to produce a customized Jackson @{@link ObjectMapper}
 * that reads and writes using single quote instead of double quotes for both fields and values,
 * with some additional Lambda-friendly methods
 */
public class Dyna53ObjectMapper extends ObjectMapper  {
    private static JsonFactory jsonFactory() {
        return new JsonFactoryBuilder()
                .quoteChar('\'')
                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                .build();
    }

    public Dyna53ObjectMapper() {
        super(jsonFactory());
    }

    public <T> T readValueRuntimeException(String content, Class<T> valueType) {
        try {
            return readValue(content, valueType);
        } catch (JsonProcessingException e) {
            throw new JsonProcessingRuntimeException(e);
        }
    }

    public String writeValueAsStringRuntimeException(Object value) {
        try {
            return writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new JsonProcessingRuntimeException(e);
        }
    }

    public class JsonProcessingRuntimeException extends RuntimeException {
        public JsonProcessingRuntimeException(Throwable cause) {
            super(cause);
        }
    }
}
