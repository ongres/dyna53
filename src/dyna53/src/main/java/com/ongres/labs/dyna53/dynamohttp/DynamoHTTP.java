/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp;


import com.ongres.labs.dyna53.dyna53.op.CreateTableManager;
import com.ongres.labs.dyna53.dyna53.op.DescribeTableManager;
import com.ongres.labs.dyna53.dynamohttp.request.CreateTableRequest;
import com.ongres.labs.dyna53.dynamohttp.request.DescribeTableRequest;
import com.ongres.labs.dyna53.dynamohttp.response.CreateTableResponse;
import com.ongres.labs.dyna53.dynamohttp.response.DescribeTableResponse;
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
    CreateTableManager createTableManager;

    @Inject
    DescribeTableManager describeTableManager;

    @POST
    public Object dynamoEntrypoint(@HeaderParam("X-Amz-Target") String amzTarget, String request) {
        String[] dynamoVersionOperation = amzTarget.split("\\.");
        if(! DYNAMO_V1_20120810.equals(dynamoVersionOperation[0])) {
            throw new BadRequestException("Unsupported API Version " + dynamoVersionOperation[0]);
        }

        var operation = DynamoOperation.getByValue(dynamoVersionOperation[1]);
        if(null == operation) {
            var message = "Invalid or unsupported operation " + "'" + dynamoVersionOperation[1] + "'";
            LOGGER.debug(message);
            throw new BadRequestException(message);
        }

        return switch (operation) {
            case CREATE_TABLE -> createTable(request);
            case DESCRIBE_TABLE -> describeTable(request);
        };
    }

    private CreateTableResponse createTable(String request) {
        var createTableRequest = jsonb.fromJson(request, CreateTableRequest.class);
        createTableManager.createTable(createTableRequest);

        var tableDescription = describeTableManager.queryTableDescription(createTableRequest.tableName());

        return new CreateTableResponse(tableDescription);
    }

    private DescribeTableResponse describeTable(String request) {
        var describeTableRequest = jsonb.fromJson(request, DescribeTableRequest.class);

        var tableDescription = describeTableManager.queryTableDescription(describeTableRequest.tableName());

        return new DescribeTableResponse(tableDescription);
    }
}
