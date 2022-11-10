/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp.response;


import com.ongres.labs.dyna53.dynamohttp.model.ConsumedCapacity;
import com.ongres.labs.dyna53.dynamohttp.model.Item;

import javax.json.bind.annotation.JsonbProperty;


public record GetItemResponse(
        @JsonbProperty("Item") Item item,
        @JsonbProperty("ConsumedCapacity") ConsumedCapacity consumedCapacity
) implements DynamoResponse {}
