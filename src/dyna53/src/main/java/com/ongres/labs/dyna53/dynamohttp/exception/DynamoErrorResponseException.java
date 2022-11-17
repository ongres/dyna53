/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp.exception;


import com.ongres.labs.dyna53.dynamohttp.model.ErrorResponse;
import com.ongres.labs.dyna53.dynamohttp.model.ErrorResponseType;

public class DynamoErrorResponseException extends DynamoException {
    private final ErrorResponseType errorResponseType;

    public DynamoErrorResponseException(ErrorResponseType errorResponseType, String message) {
        super(message);
        this.errorResponseType = errorResponseType;
    }

    public DynamoErrorResponseException(ErrorResponseType errorResponseType, String message, Throwable cause) {
        super(message, cause);
        this.errorResponseType = errorResponseType;
    }

    public ErrorResponseType getErrorResponseType() {
        return errorResponseType;
    }

    public ErrorResponse errorResponse() {
        return new ErrorResponse(errorResponseType, getMessage());
    }
}
