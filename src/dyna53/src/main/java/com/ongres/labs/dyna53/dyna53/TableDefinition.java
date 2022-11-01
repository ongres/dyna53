/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dyna53;


import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;
import java.util.Optional;


public record TableDefinition (
        @JsonbTransient String tableName,
        @JsonbProperty("hk") TableKeyDefinition hashKey,
        @JsonbProperty("rk") Optional<TableKeyDefinition> rangeKey
) {
    public TableDefinition(String tableName, TableKeyDefinition hashKey, TableKeyDefinition rangeKey) {
        this(tableName, hashKey, Optional.of(rangeKey));
    }

    public TableDefinition(String tableName, TableKeyDefinition hashKey) {
        this(tableName, hashKey, Optional.empty());
    }
}
