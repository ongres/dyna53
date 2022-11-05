/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp.response;


import com.ongres.labs.dyna53.dynamohttp.model.TableDescription;

import javax.json.bind.annotation.JsonbProperty;


public record DescribeTableResponse(
    @JsonbProperty("Table") TableDescription tableDescription
) implements DynamoResponse {}
