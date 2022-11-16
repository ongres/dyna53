/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp.exception;


import com.ongres.labs.dyna53.dynamohttp.model.ErrorResponseType;


public class ResourceInUseException extends DynamoErrorResponseException {
    public ResourceInUseException(String message) {
        super(ErrorResponseType.RESOURCE_IN_USE_EXCEPTION, message);
    }

    public ResourceInUseException(String message, Throwable cause) {
        super(ErrorResponseType.RESOURCE_IN_USE_EXCEPTION, message, cause);
    }
}
