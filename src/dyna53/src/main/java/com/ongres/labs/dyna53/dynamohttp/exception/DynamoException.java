/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp.exception;


public class DynamoException extends Exception {
    public DynamoException(String message) {
        super(message);
    }

    public DynamoException(String message, Throwable cause) {
        super(message, cause);
    }
}
