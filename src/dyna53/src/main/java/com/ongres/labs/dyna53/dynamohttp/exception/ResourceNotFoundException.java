/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp.exception;


import com.ongres.labs.dyna53.dynamohttp.model.ErrorResponseType;

public class ResourceNotFoundException extends DynamoErrorResponseException {
    public ResourceNotFoundException(String message) {
        super(ErrorResponseType.RESOURCE_NOT_FOUND_EXCEPTION, message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(ErrorResponseType.RESOURCE_NOT_FOUND_EXCEPTION, message, cause);
    }
}
