/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp;


import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


public enum DynamoOperation {
    CREATE_TABLE("CreateTable"),
    DESCRIBE_TABLE("DescribeTable"),
    PUT_ITEM("PutItem"),
    GET_ITEM("GetItem"),
    LIST_TABLES("ListTables"),
    DESCRIBE_TIME_TO_LIVE("DescribeTimeToLive"),
    SCAN("Scan")
    ;

    private final String value;

    DynamoOperation(String value) {
        this.value = value;
    }

    private static final Map<String, DynamoOperation> LOOKUP = Arrays.stream(values())
            .collect(Collectors.toMap(v -> v.value, Function.identity()));

    public static DynamoOperation getByValue(String value) {
        return LOOKUP.get(value);
    }
}
