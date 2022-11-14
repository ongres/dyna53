/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dyna53;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.ongres.labs.dyna53.dynamohttp.model.KeyAttributeType;


public record TableKeyDefinition(
        @JsonProperty("n") String keyName,
        @JsonProperty("t") KeyAttributeType keyType
){}
