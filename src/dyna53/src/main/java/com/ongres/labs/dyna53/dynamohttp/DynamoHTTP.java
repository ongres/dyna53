/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp;


import com.ongres.labs.dyna53.dyna53.Dynamo2Route53;
import com.ongres.labs.dyna53.dyna53.TableDefinition;
import com.ongres.labs.dyna53.dyna53.TableKeyDefinition;
import com.ongres.labs.dyna53.dynamohttp.model.AttributeDefinition;
import com.ongres.labs.dyna53.dynamohttp.model.KeySchema;
import com.ongres.labs.dyna53.dynamohttp.model.KeyType;
import com.ongres.labs.dyna53.dynamohttp.model.TableDescription;
import com.ongres.labs.dyna53.dynamohttp.request.CreateTableRequest;
import com.ongres.labs.dyna53.dynamohttp.request.DescribeTableRequest;
import com.ongres.labs.dyna53.dynamohttp.response.CreateTableResponse;
import com.ongres.labs.dyna53.dynamohttp.response.DescribeTableResponse;
import com.ongres.labs.dyna53.route53.Route53Manager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.Optional;


@Path("/")
public class DynamoHTTP {
    private static final String DYNAMO_V1_20120810 = "DynamoDB_20120810";

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoHTTP.class);

    @Inject
    Jsonb jsonb;

    @Inject
    Route53Manager route53Manager;

    @POST
    public Object dynamoEntrypoint(@HeaderParam("X-Amz-Target") String amzTarget, String request) {
        String[] dynamoVersionOperation = amzTarget.split("\\.");
        if(! DYNAMO_V1_20120810.equals(dynamoVersionOperation[0])) {
            throw new BadRequestException("Unsupported API Version " + dynamoVersionOperation[0]);
        }

        var operation = DynamoOperation.getByValue(dynamoVersionOperation[1]);
        if(null == operation) {
            throw new BadRequestException("Invalid or unsupported operation");
        }

        return switch (operation) {
            case CREATE_TABLE -> createTable(request);
            case DESCRIBE_TABLE -> describeTable(request);
        };
    }

    private CreateTableResponse createTable(String request) {
        LOGGER.debug("String Request: {}", request);
        var createTableRequest = jsonb.fromJson(request, CreateTableRequest.class);
        LOGGER.debug("Request as JSON: {}", createTableRequest);

        var tableDefinition = tableDefinitionFromRequest(createTableRequest);

        route53Manager.createTable(tableDefinition);
        var tableDescription = queryTableDescription(createTableRequest.tableName());

        var createTableResponse = new CreateTableResponse(tableDescription);
        LOGGER.debug("Response as JSON: {}", createTableResponse);

        return createTableResponse;
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
        var route53Label = Dynamo2Route53.mapTableName(tableName);

        return new TableDefinition(route53Label, hk, Optional.ofNullable(rk));
    }

    private TableKeyDefinition extractTableKeyDefinition(String keyName, AttributeDefinition[] attributeDefinitions) {
        for(var attributeDefinition : attributeDefinitions) {
            if(keyName.equals(attributeDefinition.attributeName())) {
                return new TableKeyDefinition(keyName, attributeDefinition.attributeType());
            }
        }

        return null;
    }

    private DescribeTableResponse describeTable(String request) {
        LOGGER.debug("String Request: {}", request);

        var describeTableRequest = jsonb.fromJson(request, DescribeTableRequest.class);
        LOGGER.debug("Request as JSON: {}", describeTableRequest);

        return new DescribeTableResponse(
                queryTableDescription(describeTableRequest.tableName())
        );
    }

    private TableDescription queryTableDescription(String tableName) {
        var tableDefinition = route53Manager.describeTable(tableName);

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
