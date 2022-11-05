/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp;


import com.ongres.labs.dyna53.dyna53.processor.PutItemProcessor;
import com.ongres.labs.dyna53.dyna53.processor.TableProcessor;
import com.ongres.labs.dyna53.dynamohttp.exception.DynamoException;
import com.ongres.labs.dyna53.dynamohttp.exception.ResourceNotFoundException;
import com.ongres.labs.dyna53.dynamohttp.model.TableDescription;
import com.ongres.labs.dyna53.dynamohttp.request.CreateTableRequest;
import com.ongres.labs.dyna53.dynamohttp.request.DescribeTableRequest;
import com.ongres.labs.dyna53.dynamohttp.request.ListTablesRequest;
import com.ongres.labs.dyna53.dynamohttp.request.PutItemRequest;
import com.ongres.labs.dyna53.dynamohttp.response.*;
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
    TableProcessor tableProcessor;

    @Inject
    PutItemProcessor putItemProcessor;

    @POST
    public DynamoResponse dynamoEntrypoint(@HeaderParam("X-Amz-Target") String amzTarget, String request)
    throws DynamoException {
        String[] dynamoVersionOperation = amzTarget.split("\\.");
        if(! DYNAMO_V1_20120810.equals(dynamoVersionOperation[0])) {
            throw new BadRequestException("Unsupported API Version " + dynamoVersionOperation[0]);
        }

        var operation = DynamoOperation.getByValue(dynamoVersionOperation[1]);
        if(null == operation) {
            var message = "Invalid or unsupported operation " + "'" + dynamoVersionOperation[1] + "'";
            LOGGER.debug(message);
            LOGGER.debug("Request[" + request + "]");
            throw new BadRequestException(message);
        }

        return switch (operation) {
            case CREATE_TABLE -> createTable(request);
            case DESCRIBE_TABLE -> describeTable(request);
            case LIST_TABLES -> listTables(request);
            case PUT_ITEM -> putItem(request);
        };
    }

    private CreateTableResponse createTable(String request) {
        var createTableRequest = jsonb.fromJson(request, CreateTableRequest.class);
        tableProcessor.createTable(createTableRequest);

        TableDescription tableDescription = null;
        try {
            tableDescription = tableProcessor.queryTableDescription(createTableRequest.tableName());
        } catch (ResourceNotFoundException e) {
            // Should not happen, table should be able to be created.
            return null;
        }

        return new CreateTableResponse(tableDescription);
    }

    private DescribeTableResponse describeTable(String request) throws ResourceNotFoundException {
        var describeTableRequest = jsonb.fromJson(request, DescribeTableRequest.class);

        var tableDescription = tableProcessor.queryTableDescription(describeTableRequest.tableName());

        return new DescribeTableResponse(tableDescription);
    }

    private ListTablesResponse listTables(String request) {
        var listTablesRequest = jsonb.fromJson(request, ListTablesRequest.class);
        var tableNames = tableProcessor.listTables(listTablesRequest);

        return new ListTablesResponse(tableNames.toArray(String[]::new));
    }

    private PutItemResponse putItem(String request) throws DynamoException {
        var putItemRequest = jsonb.fromJson(request, PutItemRequest.class);

        putItemProcessor.putItem(putItemRequest);

        return new PutItemResponse();
    }
}
