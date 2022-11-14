/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dyna53.jackson;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.ongres.labs.dyna53.dynamohttp.model.Attribute;
import com.ongres.labs.dyna53.dynamohttp.model.Item;

import java.io.IOException;


public class ItemDeserializer extends StdDeserializer<Item> {
    public ItemDeserializer() {
        super(Item.class);
    }

    @Override
    public Item deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
    throws IOException, JacksonException {
        var item = new Item();

        JsonToken jsonToken;
        while((jsonToken = jsonParser.nextToken()) != null) {
            var attributeName = jsonParser.getText();
            if(jsonParser.nextToken() == JsonToken.START_OBJECT) {
                var attribute = jsonParser.readValueAs(Attribute.class);
                item.putAttribute(attributeName, attribute);
                jsonParser.nextToken();
            }
        }

        return item;
    }
}
