/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp.jsonb;


import com.ongres.labs.dyna53.dynamohttp.model.Attribute;
import com.ongres.labs.dyna53.dynamohttp.model.AttributeType;

import javax.json.bind.serializer.DeserializationContext;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.stream.JsonParser;
import java.lang.reflect.Type;


public class AttributeDeserializer implements JsonbDeserializer<Attribute> {
    @Override
    public Attribute deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
        while(parser.hasNext()) {
            if(parser.next() == JsonParser.Event.KEY_NAME) {
                var type = AttributeType.valueOf(parser.getString());
                if(null != type && parser.hasNext()) {
                    parser.next();
                    var value = parser.getString();

                    return new Attribute(type, value);
                }
            }
        }

        return null;
    }
}
