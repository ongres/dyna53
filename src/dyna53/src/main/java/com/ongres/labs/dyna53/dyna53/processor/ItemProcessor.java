/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dyna53.processor;


import com.ongres.labs.dyna53.dyna53.Dynamo2Route53;
import com.ongres.labs.dyna53.dyna53.TableDefinitionCache;
import com.ongres.labs.dyna53.dyna53.TableKeyDefinition;
import com.ongres.labs.dyna53.dynamohttp.exception.DynamoException;
import com.ongres.labs.dyna53.dynamohttp.exception.ResourceNotFoundException;
import com.ongres.labs.dyna53.dynamohttp.exception.ValidationException;
import com.ongres.labs.dyna53.dynamohttp.model.Item;
import com.ongres.labs.dyna53.dynamohttp.request.PutItemRequest;
import com.ongres.labs.dyna53.dynamohttp.request.ScanRequest;
import com.ongres.labs.dyna53.route53.Route53Manager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import java.util.stream.Stream;


@ApplicationScoped
public class ItemProcessor {
    @Inject
    Route53Manager route53Manager;

    @Inject
    Dynamo2Route53 dynamo2Route53;

    @Inject
    TableDefinitionCache tableDefinitionCache;

    @Inject
    Jsonb jsonb;

    public void putItem(PutItemRequest putItemRequest) throws DynamoException {
        var dyna53TableName = Dynamo2Route53.toValidRoute53Label(putItemRequest.tableName());
        var tableDefinitionOptional = tableDefinitionCache.tableDefinition(dyna53TableName);
        if(tableDefinitionOptional.isEmpty()) {
            throw new ResourceNotFoundException("Requested resource not found");
        }
        var tableDefinition = tableDefinitionOptional.get();

        // Validate the request contains the hk and rk if applicable
        var item = putItemRequest.item();
        var hashKey = tableDefinition.getHashKey();
        validateKey(item, hashKey);

        var rangeKey = tableDefinition.getRangeKey();
        if(rangeKey.isPresent()) {
            validateKey(item, rangeKey.get());
        }

        // Insert the item
        var serializedItem = dynamo2Route53.serializeResourceRecord(
                jsonb.toJson(item, Item.class)
        );
        var hkLabel = Dynamo2Route53.hashHK2label(
                item.getAttribute(hashKey.keyName()).get().value()
        );
        route53Manager.createSingleValuedTXTResource(hkLabel, dyna53TableName, serializedItem);
    }

    private void validateKey(Item item, TableKeyDefinition tableKeyDefinition) throws ValidationException {
        var itemAttribute = item.getAttribute(tableKeyDefinition.keyName());
        if(itemAttribute.isEmpty()) {
            throw new ValidationException(
                    "One or more parameter values were invalid: Missing the key " + tableKeyDefinition.keyName() +
                            " in the item"
            );
        }

        if(! tableKeyDefinition.keyType().matchesAttributeType(itemAttribute.get().type())) {
            throw new ValidationException(
                    "One or more parameter values were invalid: Type mismatch for key " + tableKeyDefinition.keyName() +
                    " expected: " + tableKeyDefinition.keyType() + " actual: " + itemAttribute.get().type()
            );
        }
    }

    public Stream<Item> scan(ScanRequest scanRequest) {
        return route53Manager.listTXTRecordsWithLabel(
                scanRequest.tableName(), Dynamo2Route53.ITEM_ROUTE53_LABEL_REGEX
        ).map(value -> dynamo2Route53.deserializeResourceRecord(value))
        .map(value -> jsonb.fromJson(value, Item.class));
    }
}
