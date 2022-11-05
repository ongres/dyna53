/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp.exception;


import com.ongres.labs.dyna53.dynamohttp.model.ErrorResponseType;

public class ValidationException extends DynamoErrorResponseException {
    public ValidationException(String message) {
        super(ErrorResponseType.VALIDATION_EXCEPTION, message);
    }

    public ValidationException(String message, Throwable cause) {
        super(ErrorResponseType.VALIDATION_EXCEPTION, message, cause);
    }
}
