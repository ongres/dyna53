/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dyna53;


import com.ongres.labs.dyna53.dyna53.processor.TableProcessor;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


@ApplicationScoped
public class TableDefinitionCache {
    @Inject
    TableProcessor tableProcessor;

    private final Map<String,TableDefinition> tableDefinitions = new ConcurrentHashMap<>();

    public void cacheTableDefinition(String tableName, TableDefinition tableDefinition) {
        tableDefinitions.put(tableName, tableDefinition);
    }

    public Optional<TableDefinition> tableDefinition(String tableName) {
        var dyna53TableName = Dynamo2Route53.toValidRoute53Label(tableName);
        var cachedTableDefinition = tableDefinitions.get(dyna53TableName);

        if(null != cachedTableDefinition) {
            return Optional.of(cachedTableDefinition);
        } else {
            var tableDefinitionOptional = tableProcessor.tableDefinitionFromRoute53(dyna53TableName);

            tableDefinitionOptional.ifPresent(
                    tableDefinition -> cacheTableDefinition(dyna53TableName, tableDefinition)
            );

            return tableDefinitionOptional;
        }
    }


}
