/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dyna53;


import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;
import java.util.Optional;


/* A regular POJO class instead of a record is used here to avoid Jsonb problems with null fields ("rk").
 * At least with the current Jsonb version used, if rk is not present deserialization fails.
 * Switching to POJO and using the empty constructor annotated with @JsonbCreator removes this problem.
 */
public class TableDefinition {
    @JsonbTransient private String tableName;
    @JsonbProperty("hk") private TableKeyDefinition hashKey;
    @JsonbProperty("rk") private Optional<TableKeyDefinition> rangeKey = Optional.empty();

    @JsonbCreator
    public TableDefinition() {}

    public TableDefinition(String tableName, TableKeyDefinition hashKey, Optional<TableKeyDefinition> rangeKey) {
        this.tableName = tableName;
        this.hashKey = hashKey;
        this.rangeKey = rangeKey;
    }

    public String getTableName() {
        return tableName;
    }

    public TableKeyDefinition getHashKey() {
        return hashKey;
    }

    public Optional<TableKeyDefinition> getRangeKey() {
        return rangeKey;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setHashKey(TableKeyDefinition hashKey) {
        this.hashKey = hashKey;
    }

    public void setRangeKey(TableKeyDefinition rangeKey) {
        this.rangeKey = Optional.of(rangeKey);
    }
}
