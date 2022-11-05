/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp.jsonb;


import com.ongres.labs.dyna53.dynamohttp.model.Attribute;
import com.ongres.labs.dyna53.dynamohttp.model.Item;

import javax.json.bind.serializer.DeserializationContext;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.stream.JsonParser;
import java.lang.reflect.Type;


public class ItemDeserializer implements JsonbDeserializer<Item> {
    @Override
    public Item deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
        var item = new Item();
        while(parser.hasNext()) {
            if(parser.next() == JsonParser.Event.KEY_NAME) {
                var attributeName = parser.getString();
                if(parser.hasNext() && parser.next() == JsonParser.Event.START_OBJECT) {
                    var attribute = ctx.deserialize(Attribute.class, parser);
                    item.putAttribute(attributeName, attribute);
                }
            }
        }

        return  item;
    }
}
