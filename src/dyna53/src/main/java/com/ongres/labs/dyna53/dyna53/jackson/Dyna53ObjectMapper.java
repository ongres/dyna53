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
 * that reads and writes using single quote instead of double quotes for both fields and values.
 * It also performs a mapping from the JSON-escaped single quote character (') to the first unused character in the
 * ASCII table, as standard JSON escape sequences are not valid in Route53.
 * It also provides some convenient Lambda-friendly methods.
 */
public class Dyna53ObjectMapper extends ObjectMapper  {
    private static final int FIRST_UNUSED_ASCII_CHAR = 129;

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
            var adaptedContent = content
                    // We need to properly escape backslash with another backslash;
                    // but undo that if it was a double quote escaped!
                    .replace("\\", "\\\\").replace("\\\\\"", "\\\"")

                    // Map the first unused ASCII char back to an JSON escaped single quote
                    .replace("" + ((char) FIRST_UNUSED_ASCII_CHAR), "\\u0027")
                    ;
            return readValue(adaptedContent, valueType);
        } catch (JsonProcessingException e) {
            throw new JsonProcessingRuntimeException(e);
        }
    }

    public String writeValueAsStringRuntimeException(Object value) {
        try {
            return writeValueAsString(value)
                    // Jackson seems not to be doing its job correctly when the quote char is different from double quote
                    // Jackson is still escaping double quote when it shouldn't, so we fix it here
                    .replace("\\\"", "\"")

                    // Map escaped single quote to the first unused ASCII char
                    .replace("\\u0027", "\\" + Integer.toOctalString(FIRST_UNUSED_ASCII_CHAR))

                    // Backslash doesn't need to (must not be) escaped
                    .replace("\\\\", "\\")
                    ;
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
