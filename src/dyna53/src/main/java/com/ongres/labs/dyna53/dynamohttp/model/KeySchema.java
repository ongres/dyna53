/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp.model;


import javax.json.bind.annotation.JsonbProperty;


public record KeySchema(
        @JsonbProperty("AttributeName") String attributeName,
        @JsonbProperty("KeyType") KeyType keyType
) {}
