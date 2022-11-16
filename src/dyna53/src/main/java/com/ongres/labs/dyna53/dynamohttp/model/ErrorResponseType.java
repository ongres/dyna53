/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp.model;


import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum ErrorResponseType {
    RESOURCE_NOT_FOUND_EXCEPTION("com.amazonaws.dynamodb.v20120810#ResourceNotFoundException"),
    RESOURCE_IN_USE_EXCEPTION("com.amazonaws.dynamodb.v20120810#ResourceInUseException"),
    PROVISIONED_THROUGHPUT_EXCEEDED_EXCEPTION("com.amazonaws.dynamodb.v20111205#ProvisionedThroughputExceededException"),
    VALIDATION_EXCEPTION("com.amazon.coral.validate#ValidationException")
    ;

    private static final Map<String,ErrorResponseType> INSTANCES_BY_VALUE = Arrays
            .stream(values())
            .collect(
                    Collectors.toUnmodifiableMap(v -> v.toString(), v -> v)
            );

    private final String value;

    ErrorResponseType(String value) {
        this.value = value;
    }

    public static ErrorResponseType from(String value) {
        return INSTANCES_BY_VALUE.get(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
