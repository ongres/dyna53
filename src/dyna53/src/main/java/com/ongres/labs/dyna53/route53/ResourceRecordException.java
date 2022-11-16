/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.route53;


public class ResourceRecordException extends Exception {
    public ResourceRecordException(String message) {
        super(message);
    }

    public static ResourceRecordException valueAlreadyExistsException() {
        return new ResourceRecordException("Value already exists");
    }
}
