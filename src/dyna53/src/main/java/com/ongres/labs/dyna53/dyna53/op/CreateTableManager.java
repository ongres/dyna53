/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dyna53.op;


import com.ongres.labs.dyna53.dyna53.Dynamo2Route53;
import com.ongres.labs.dyna53.dyna53.TableDefinition;
import com.ongres.labs.dyna53.dyna53.TableKeyDefinition;
import com.ongres.labs.dyna53.dynamohttp.model.AttributeDefinition;
import com.ongres.labs.dyna53.dynamohttp.request.CreateTableRequest;
import com.ongres.labs.dyna53.route53.Route53Manager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;


@ApplicationScoped
public class CreateTableManager {
    @Inject
    Route53Manager route53Manager;

    @Inject
    Dynamo2Route53 dynamo2Route53;

    public void createTable(CreateTableRequest createTableRequest) {
        var tableDefinition = tableDefinitionFromRequest(createTableRequest);
        var serializedTableDefinition = dynamo2Route53.serializeTableDefinition(tableDefinition);
        route53Manager.createSingleValuedResource(tableDefinition.getTableName(), serializedTableDefinition);
    }

    private TableDefinition tableDefinitionFromRequest(CreateTableRequest createTableRequest) {
        TableKeyDefinition hk = null;
        TableKeyDefinition rk = null;
        for(var keySchema : createTableRequest.keySchema()) {
            var tableKeyDefinition = extractTableKeyDefinition(
                    keySchema.attributeName(),
                    createTableRequest.attributeDefinitions()
            );
            assert tableKeyDefinition != null;

            switch (keySchema.keyType()) {
                case HASH -> hk = tableKeyDefinition;
                case RANGE -> rk = tableKeyDefinition;
            }
        }

        assert hk != null;

        // Map request to dyna53 data model, suitable for writing into Route53
        var tableName = createTableRequest.tableName();
        var route53Label = Dynamo2Route53.mapTableName(tableName);

        return new TableDefinition(route53Label, hk, Optional.ofNullable(rk));
    }

    private TableKeyDefinition extractTableKeyDefinition(String keyName, AttributeDefinition[] attributeDefinitions) {
        for(var attributeDefinition : attributeDefinitions) {
            if(keyName.equals(attributeDefinition.attributeName())) {
                return new TableKeyDefinition(keyName, attributeDefinition.attributeType());
            }
        }

        return null;
    }
}
