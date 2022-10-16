/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp.request;


import com.ongres.labs.dyna53.dynamohttp.model.AttributeDefinition;
import com.ongres.labs.dyna53.dynamohttp.model.KeySchema;

import javax.json.bind.annotation.JsonbProperty;
import java.util.Arrays;


public record CreateTableRequest (
    @JsonbProperty("AttributeDefinitions") AttributeDefinition[] attributeDefinitions,
    @JsonbProperty("KeySchema") KeySchema[] keySchema,
    @JsonbProperty("TableName") String tableName
) {
    @Override
    public String toString() {
        return "CreateTableRequest{" +
                "attributeDefinitions=" + Arrays.toString(attributeDefinitions) +
                ", keySchema=" + Arrays.toString(keySchema) +
                ", tableName='" + tableName + '\'' +
                '}';
    }
}
