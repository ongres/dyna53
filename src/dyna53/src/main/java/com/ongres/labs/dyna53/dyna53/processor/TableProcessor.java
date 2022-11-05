/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dyna53.processor;


import com.ongres.labs.dyna53.dyna53.Dynamo2Route53;
import com.ongres.labs.dyna53.dyna53.TableDefinition;
import com.ongres.labs.dyna53.dyna53.TableDefinitionCache;
import com.ongres.labs.dyna53.dyna53.TableKeyDefinition;
import com.ongres.labs.dyna53.dynamohttp.exception.ResourceNotFoundException;
import com.ongres.labs.dyna53.dynamohttp.model.AttributeDefinition;
import com.ongres.labs.dyna53.dynamohttp.model.KeySchema;
import com.ongres.labs.dyna53.dynamohttp.model.KeyType;
import com.ongres.labs.dyna53.dynamohttp.model.TableDescription;
import com.ongres.labs.dyna53.dynamohttp.request.CreateTableRequest;
import com.ongres.labs.dyna53.dynamohttp.request.ListTablesRequest;
import com.ongres.labs.dyna53.route53.Route53Manager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;
import java.util.stream.Stream;


@ApplicationScoped
public class TableProcessor {
    @Inject
    Route53Manager route53Manager;

    @Inject
    Dynamo2Route53 dynamo2Route53;

    @Inject
    TableDefinitionCache tableDefinitionCache;

    public void createTable(CreateTableRequest createTableRequest) {
        var tableDefinition = tableDefinitionFromRequest(createTableRequest);
        var serializedTableDefinition = dynamo2Route53.serializeTableDefinition(tableDefinition);
        route53Manager.createDummySRVResource(tableDefinition.getTableName(), serializedTableDefinition);
    }

    private TableDefinition tableDefinitionFromRequest(CreateTableRequest createTableRequest) {
        TableKeyDefinition hk = null;
        TableKeyDefinition rk = null;
        for(var keySchema : createTableRequest.keySchema()) {
            var tableKeyDefinition = extractTableKeyDefinition(
                    keySchema.attributeName(),
                    createTableRequest.attributeDefinitions()
            );
            assert tableKeyDefinition != null;

            switch (keySchema.keyType()) {
                case HASH -> hk = tableKeyDefinition;
                case RANGE -> rk = tableKeyDefinition;
            }
        }

        assert hk != null;

        // Map request to dyna53 data model, suitable for writing into Route53
        var tableName = createTableRequest.tableName();
        var route53Label = Dynamo2Route53.toValidRoute53Label(tableName);

        return new TableDefinition(route53Label, hk, Optional.ofNullable(rk));
    }

    private TableKeyDefinition extractTableKeyDefinition(String keyName, AttributeDefinition[] attributeDefinitions) {
        for(var attributeDefinition : attributeDefinitions) {
            if(keyName.equals(attributeDefinition.attributeName())) {
                return new TableKeyDefinition(keyName, attributeDefinition.keyAttributeType());
            }
        }

        return null;
    }

    public Optional<TableDefinition> tableDefinitionFromRoute53(String label) {
        return route53Manager
                .getSingleValuedSRVResource(label)
                .map(
                        serializedTableDefinition -> dynamo2Route53.deserializeTableDefinition(
                                label, serializedTableDefinition
                        )
                );
    }

    public TableDescription queryTableDescription(String tableName) throws ResourceNotFoundException {
        return tableDefinitionCache.tableDefinition(tableName)
                .map(tableDefinition -> tableDescriptionFromTableDefinition(tableDefinition))
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Requested resource not found: Table: " + tableName + " not found"
                        )
                );
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

    public Stream<String> listTables(ListTablesRequest listTablesRequest) {
        // listTablesRequest is ignored for now, full list is returned, sorry!

        return route53Manager.listSRVRecordsLabel();
    }
}
