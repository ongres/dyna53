/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp.exception;


import com.ongres.labs.dyna53.dynamohttp.model.ErrorResponse;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import javax.ws.rs.core.Response;


public class ExceptionMappers {
    private RestResponse<ErrorResponse> mapException(DynamoErrorResponseException exception, Response.Status status) {
        return RestResponse.status(
                status,
                exception.errorResponse()
        );
    }
    @ServerExceptionMapper
    public RestResponse<ErrorResponse> mapDynamoException(DynamoErrorResponseException exception) {
        return mapException(exception, Response.Status.BAD_REQUEST);
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> mapInternalErrorException(InternalErrorException exception) {
        return mapException(exception, Response.Status.INTERNAL_SERVER_ERROR);
    }
}
