/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp.exception;


import com.ongres.labs.dyna53.dynamohttp.model.ErrorResponseType;

public class ProvisionedThroughputExceededException extends DynamoErrorResponseException {
    public ProvisionedThroughputExceededException(String message) {
        super(ErrorResponseType.PROVISIONED_THROUGHPUT_EXCEEDED_EXCEPTION, message);
    }

    public ProvisionedThroughputExceededException(String message, Throwable cause) {
        super(ErrorResponseType.PROVISIONED_THROUGHPUT_EXCEEDED_EXCEPTION, message, cause);
    }
}
