/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp.request;


import com.ongres.labs.dyna53.dynamohttp.model.Item;

import javax.json.bind.annotation.JsonbProperty;

public record GetItemRequest(
        @JsonbProperty("TableName") String tableName,
        @JsonbProperty("Key") Item key
) {}
