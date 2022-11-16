/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dyna53.processor;


import com.ongres.labs.dyna53.dyna53.*;
import com.ongres.labs.dyna53.dynamohttp.exception.DynamoException;
import com.ongres.labs.dyna53.dynamohttp.exception.ProvisionedThroughputExceededException;
import com.ongres.labs.dyna53.dynamohttp.exception.ResourceInUseException;
import com.ongres.labs.dyna53.dynamohttp.exception.ResourceNotFoundException;
import com.ongres.labs.dyna53.dynamohttp.model.*;
import com.ongres.labs.dyna53.dynamohttp.request.CreateTableRequest;
import com.ongres.labs.dyna53.dynamohttp.request.DescribeTimeToLiveRequest;
import com.ongres.labs.dyna53.dynamohttp.request.ListTablesRequest;
import com.ongres.labs.dyna53.route53.ResourceRecordException;
import com.ongres.labs.dyna53.route53.Route53Manager;
import com.ongres.labs.dyna53.route53.TimeoutException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;
import java.util.stream.Stream;


@ApplicationScoped
public class TableProcessor {
    // Table definition is stored as a SRV records. They require a priority, weight, port --which are ignored by dyna53
    private static final int ROUTE53_DUMMY_SRV_RECORD_PRIORITY = 80;
    private static final int ROUTE53_DUMMY_SRV_RECORD_WEIGHT = 71;
    private static final int ROUTE53_DUMMY_SRV_RECORD_PORT = 5432;

    @Inject
    Route53Manager route53Manager;

    @Inject
    Dynamo2Route53 dynamo2Route53;

    @Inject
    TableDefinitionCache tableDefinitionCache;

    public void createTable(CreateTableRequest createTableRequest) throws DynamoException {
        var tableDefinition = tableDefinitionFromRequest(createTableRequest);
        var serializedTableKeysDefinition = dynamo2Route53.serializeTableDefinition(tableDefinition);
        try {
            route53Manager.createSRVResource(
                    tableDefinition.tableName(),
                    serializedTableKeysDefinition,
                    ROUTE53_DUMMY_SRV_RECORD_PRIORITY,
                    ROUTE53_DUMMY_SRV_RECORD_WEIGHT,
                    ROUTE53_DUMMY_SRV_RECORD_PORT
            );
        } catch (TimeoutException e) {
            throw new ProvisionedThroughputExceededException("Operation is retryable");
        } catch (ResourceRecordException e) {
            throw new ResourceInUseException("Table already exists: " + tableDefinition.tableName());
        }
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

        return new TableDefinition(
                route53Label,
                new TableKeysDefinition(hk, Optional.ofNullable(rk))
        );
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
                .getSingleValuedSRVResource(
                        ROUTE53_DUMMY_SRV_RECORD_PRIORITY,
                        ROUTE53_DUMMY_SRV_RECORD_WEIGHT,
                        ROUTE53_DUMMY_SRV_RECORD_PORT,
                        label
                )
                .map(
                        serializedTableDefinition -> dynamo2Route53.deserializeTableDefinition(
                                label, serializedTableDefinition
                        )
                );
    }

    public TableDefinition tableDefinition(String tableName) throws ResourceNotFoundException {
        return tableDefinitionCache.tableDefinition(tableName)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Requested resource not found: Table: " + tableName + " not found"
                        )
                );
    }

    public void checkTableIsValid(String tableName) throws ResourceNotFoundException {
        tableDefinition(tableName);
    }

    public TableDescription queryTableDescription(String tableName) throws ResourceNotFoundException {
        return tableDescriptionFromTableDefinition(
                tableDefinition(tableName)
        );
    }

    private TableDescription tableDescriptionFromTableDefinition(TableDefinition tableDefinition) {
        AttributeDefinition[] attributeDefinitions;
        KeySchema[] keySchema;
        var keysDefinition = tableDefinition.keysDefinition();
        var hashKeyDefinition = keysDefinition.hashKey();
        var rangeKeyDefinition = keysDefinition.rangeKey();

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

        return new TableDescription(tableDefinition.tableName(), attributeDefinitions, keySchema);
    }

    public Stream<String> listTables(ListTablesRequest listTablesRequest) {
        // listTablesRequest is ignored for now, full list is returned, sorry!

        return route53Manager.listSRVRecordsLabel(
                ROUTE53_DUMMY_SRV_RECORD_PRIORITY, ROUTE53_DUMMY_SRV_RECORD_WEIGHT, ROUTE53_DUMMY_SRV_RECORD_PORT
        );
    }

    public TimeToLiveDescription timeToLiveDescription(DescribeTimeToLiveRequest describeTimeToLiveRequest)
    throws ResourceNotFoundException {
        // Find table definition, otherwise it throws ResourceNotFoundException
        checkTableIsValid(describeTimeToLiveRequest.tableName());

        // Method is not really implemented, return always DISABLED
        return new TimeToLiveDescription(TimeToLiveStatus.DISABLED);
    }
}
