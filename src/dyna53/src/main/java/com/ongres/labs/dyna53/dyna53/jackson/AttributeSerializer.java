/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dyna53.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.ongres.labs.dyna53.dynamohttp.model.Attribute;

import java.io.IOException;


public class AttributeSerializer extends StdSerializer<Attribute> {
    public AttributeSerializer() {
        super(Attribute.class);
    }

    @Override
    public void serialize(Attribute attribute, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
    throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField(attribute.type().name(), attribute.value());
        jsonGenerator.writeEndObject();
    }
}
