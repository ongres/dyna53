/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dyna53.op;


import com.ongres.labs.dyna53.dyna53.Dynamo2Route53;
import com.ongres.labs.dyna53.dyna53.TableDefinition;
import com.ongres.labs.dyna53.dynamohttp.model.AttributeDefinition;
import com.ongres.labs.dyna53.dynamohttp.model.KeySchema;
import com.ongres.labs.dyna53.dynamohttp.model.KeyType;
import com.ongres.labs.dyna53.dynamohttp.model.TableDescription;
import com.ongres.labs.dyna53.route53.Route53Manager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;


@ApplicationScoped
public class DescribeTableManager {
    @Inject
    Route53Manager route53Manager;

    @Inject
    Dynamo2Route53 dynamo2Route53;

    public TableDescription queryTableDescription(String tableName) {
        var label = Dynamo2Route53.mapTableName(tableName);

        var serializedTableDefinition = route53Manager.getSingleValuedResource(label);

        var tableDefinition = dynamo2Route53.deserializeTableDefinition(serializedTableDefinition);
        tableDefinition.setTableName(tableName);

        return tableDescriptionFromTableDefinition(tableDefinition);
    }

    private TableDescription tableDescriptionFromTableDefinition(TableDefinition tableDefinition) {
        AttributeDefinition[] attributeDefinitions;
        KeySchema[] keySchema;
        var hashKeyDefinition = tableDefinition.getHashKey();
        var rangeKeyDefinition = tableDefinition.getRangeKey();

        if(rangeKeyDefinition.isPresent()) {
            attributeDefinitions = new AttributeDefinition[] {
                    new AttributeDefinition(hashKeyDefinition.keyName(), hashKeyDefinition.keyType()),
                    new AttributeDefinition(rangeKeyDefinition.get().keyName(), rangeKeyDefinition.get().keyType())
            };
            keySchema = new KeySchema[] {
                    new KeySchema(hashKeyDefinition.keyName(), KeyType.HASH),
                    new KeySchema(rangeKeyDefinition.get().keyName(), KeyType.RANGE)
            };
        } else {
            attributeDefinitions = new AttributeDefinition[] {
                    new AttributeDefinition(hashKeyDefinition.keyName(), hashKeyDefinition.keyType())
            };
            keySchema = new KeySchema[] {
                    new KeySchema(hashKeyDefinition.keyName(), KeyType.HASH)
            };
        }

        return new TableDescription(tableDefinition.getTableName(), attributeDefinitions, keySchema);
    }
}
