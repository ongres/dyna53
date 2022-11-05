/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp.jsonb;


import com.ongres.labs.dyna53.dynamohttp.model.ErrorResponseType;

import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;


public class ErrorResponseTypeSerializer implements JsonbSerializer<ErrorResponseType> {
    @Override
    public void serialize(ErrorResponseType obj, JsonGenerator generator, SerializationContext ctx) {
        generator.write(obj.toString());
    }
}
