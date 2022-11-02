/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp.request;


import javax.json.bind.annotation.JsonbProperty;


public record DescribeTableRequest(
    @JsonbProperty("TableName") String tableName
) {
}
