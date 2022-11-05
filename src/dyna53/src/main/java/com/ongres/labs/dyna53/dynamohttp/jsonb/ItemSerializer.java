/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp.jsonb;


import com.ongres.labs.dyna53.dynamohttp.model.Item;

import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;


public class ItemSerializer implements JsonbSerializer<Item> {
    @Override
    public void serialize(Item item, JsonGenerator generator, SerializationContext ctx) {
        generator.writeStartObject();
        item.attributes((attributeName, attribute) -> {
            generator.writeKey(attributeName);
            generator.writeStartObject();
            generator.write(attribute.type().toString(), attribute.value());
            generator.writeEnd();
        });
        generator.writeEnd();
    }
}
