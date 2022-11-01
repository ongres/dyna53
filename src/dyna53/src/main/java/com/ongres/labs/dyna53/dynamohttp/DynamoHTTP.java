/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp;


import com.ongres.labs.dyna53.dyna53.Dynamo2Route53;
import com.ongres.labs.dyna53.dyna53.TableDefinition;
import com.ongres.labs.dyna53.dyna53.TableKeyDefinition;
import com.ongres.labs.dyna53.dynamohttp.model.AttributeType;
import com.ongres.labs.dyna53.dynamohttp.model.TableDescription;
import com.ongres.labs.dyna53.dynamohttp.request.CreateTableRequest;
import com.ongres.labs.dyna53.dynamohttp.response.CreateTableResponse;
import com.ongres.labs.dyna53.route53.Route53Manager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;


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
        };
    }

    private Object createTable(String request) {
        LOGGER.debug("String Request: {}", request);
        var createTableRequest = jsonb.fromJson(request, CreateTableRequest.class);
        LOGGER.debug("Request as JSON: {}", createTableRequest);

        // Map request to dyna53 data model, suitable for writing into Route53
        var tableName = createTableRequest.tableName();
        var route53Label = Dynamo2Route53.mapTableName(tableName);
        var hashKey = new TableKeyDefinition(route53Label, AttributeType.S);
        var tableDefinition = new TableDefinition(tableName, hashKey);

        var route53Response = route53Manager.createTable(tableDefinition);
        var tableDescription = new TableDescription(route53Response);

        var createTableResponse = new CreateTableResponse(tableDescription);
        LOGGER.debug("Response as JSON: {}", createTableResponse);

        return createTableResponse;
    }
}
