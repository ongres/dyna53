/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp.model;


import javax.json.bind.annotation.JsonbProperty;


public record AttributeDefinition(
        @JsonbProperty("AttributeName") String attributeName,
        @JsonbProperty("AttributeType") KeyAttributeType keyAttributeType
) {}
