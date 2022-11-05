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
    @ServerExceptionMapper
    public RestResponse<ErrorResponse> mapDynamoException(DynamoErrorResponseException exception) {
        return RestResponse.status(
                Response.Status.BAD_REQUEST,
                new ErrorResponse(exception.getErrorResponseType(), exception.getMessage())
        );
    }
}
