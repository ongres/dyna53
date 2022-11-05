/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp.model;


public enum KeyAttributeType {
    S(AttributeType.S),
    N(AttributeType.N),
    B(AttributeType.B);

    private final AttributeType attributeType;

    KeyAttributeType(AttributeType attributeType) {
        this.attributeType = attributeType;
    }

    public boolean matchesAttributeType(AttributeType attributeType) {
        return attributeType != null && this.attributeType == attributeType;
    }
}
