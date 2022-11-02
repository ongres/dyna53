/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp.model;


import javax.json.bind.annotation.JsonbProperty;


public record TableDescription (
    @JsonbProperty("TableName") String tableName,
    @JsonbProperty("TableStatus") TableStatus tableStatus,
    @JsonbProperty("AttributeDefinitions") AttributeDefinition[] attributeDefinitions,
    @JsonbProperty("KeySchema") KeySchema[] keySchema
) {
    public TableDescription(String tableName, AttributeDefinition[] attributeDefinitions, KeySchema[] keySchema) {
        this(tableName, TableStatus.ACTIVE, attributeDefinitions, keySchema);
    }
}
