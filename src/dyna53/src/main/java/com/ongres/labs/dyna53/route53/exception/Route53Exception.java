/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.route53.exception;


public class Route53Exception extends Exception {
    public Route53Exception() {}

    public Route53Exception(String message) {
        super(message);
    }
}