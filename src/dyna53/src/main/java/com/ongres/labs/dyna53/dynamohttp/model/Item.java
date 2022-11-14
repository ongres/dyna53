/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp.model;


import com.fasterxml.jackson.annotation.JsonAnyGetter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;


public class Item {
    private final Map<String, Attribute> itemTypeValues = new HashMap<>();

    public Optional<Attribute> getAttribute(String attributeName) {
        return Optional.ofNullable(itemTypeValues.get(attributeName));
    }

    public void putAttribute(String attributeName, Attribute attribute) {
        itemTypeValues.put(attributeName, attribute);
    }

    public void attributes(BiConsumer<String,Attribute> attributesConsumer) {
        itemTypeValues.forEach(attributesConsumer);
    }

    @JsonAnyGetter
    public Map<String, Attribute> itemMap() {
        return Collections.unmodifiableMap(itemTypeValues);
    }
}
