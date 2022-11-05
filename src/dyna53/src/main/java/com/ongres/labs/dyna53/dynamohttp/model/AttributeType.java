/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp.model;


public enum AttributeType {
    // TODO: for now, only scalar types are supported (or at least, tested)
    B,
    BOOL,
    N,
    NULL,
    S;
}
