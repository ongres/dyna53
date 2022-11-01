/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dyna53;


import com.ongres.labs.dyna53.dynamohttp.model.AttributeType;

import javax.json.bind.annotation.JsonbProperty;


public record TableKeyDefinition(
        @JsonbProperty("n") String keyName,
        @JsonbProperty("t") AttributeType keyType
){}
