/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp.exception;


import com.ongres.labs.dyna53.dynamohttp.model.ErrorResponseType;

public class InternalErrorException extends DynamoErrorResponseException {
    public InternalErrorException(String message) {
        super(ErrorResponseType.INTERNAL_SERVER_ERROR, message);
    }

    public InternalErrorException(String message, Throwable cause) {
        super(ErrorResponseType.INTERNAL_SERVER_ERROR, message, cause);
    }
}
