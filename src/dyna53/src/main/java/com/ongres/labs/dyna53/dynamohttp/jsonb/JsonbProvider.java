/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp.jsonb;


import javax.enterprise.inject.Produces;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;


public class JsonbProvider {
    @Produces
    public Jsonb jsonbProvider() {
        JsonbConfig config = new JsonbConfig()
                .withSerializers(
                        new ErrorResponseTypeSerializer(),
                        new ItemSerializer()
                ).withDeserializers(
                        new AttributeDeserializer(),
                        new ItemDeserializer()
                );

        return JsonbBuilder.create(config);
    }
}
