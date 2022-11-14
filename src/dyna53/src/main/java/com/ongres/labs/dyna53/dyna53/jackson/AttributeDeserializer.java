/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dyna53.jackson;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.util.RequestPayload;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.ongres.labs.dyna53.dynamohttp.model.Attribute;
import com.ongres.labs.dyna53.dynamohttp.model.AttributeType;

import java.io.IOException;


public class AttributeDeserializer extends StdDeserializer<Attribute> {
    public AttributeDeserializer() {
        super(Attribute.class);
    }

    @Override
    public Attribute deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
    throws IOException, JacksonException {
        if(jsonParser.nextToken() != JsonToken.FIELD_NAME) {
            throw new JsonParseException(jsonParser, "Invalid JSON for " + Attribute.class.getName());
        }
        var attributeType = AttributeType.valueOf(jsonParser.getText());
        var value = jsonParser.nextTextValue();

        return new Attribute(attributeType, value);
    }
}
