/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dyna53.jackson;


import com.fasterxml.jackson.databind.module.SimpleModule;
import com.ongres.labs.dyna53.dynamohttp.model.Attribute;
import com.ongres.labs.dyna53.dynamohttp.model.Item;
import io.quarkus.jackson.ObjectMapperCustomizer;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;


public class ObjectMapperProvider {
    @Singleton
    @Produces
    public Dyna53ObjectMapper objectMapper(Instance<ObjectMapperCustomizer> customizers) {
        var dyna53ObjectMapper = new Dyna53ObjectMapper();

        // Apply all ObjectMapperCustomizer beans (incl. Quarkus) @see https://quarkus.io/guides/rest-json#json
        for (ObjectMapperCustomizer customizer : customizers) {
            customizer.customize(dyna53ObjectMapper);
        }

        registerCustomSerializersDeserializers(dyna53ObjectMapper);

        return dyna53ObjectMapper;
    }

    private void registerCustomSerializersDeserializers(Dyna53ObjectMapper dyna53ObjectMapper) {
        var module = new SimpleModule();

        module.addSerializer(Attribute.class, new AttributeSerializer());
        module.addDeserializer(Attribute.class, new AttributeDeserializer());
        module.addDeserializer(Item.class, new ItemDeserializer());

        dyna53ObjectMapper.registerModule(module);
    }
}
