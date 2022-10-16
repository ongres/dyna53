/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp;


import com.ongres.labs.dyna53.dynamohttp.model.TableDescription;
import com.ongres.labs.dyna53.dynamohttp.request.CreateTableRequest;
import com.ongres.labs.dyna53.dynamohttp.response.CreateTableResponse;
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

        var tableDescription = new TableDescription(createTableRequest.tableName());

        var createTableResponse = new CreateTableResponse(tableDescription);
        LOGGER.debug("Response as JSON: {}", createTableResponse);


        return createTableResponse;
    }
}
