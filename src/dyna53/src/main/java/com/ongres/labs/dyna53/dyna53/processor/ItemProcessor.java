/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dyna53.processor;


import com.ongres.labs.dyna53.dyna53.Dynamo2Route53;
import com.ongres.labs.dyna53.dyna53.TableDefinition;
import com.ongres.labs.dyna53.dyna53.TableDefinitionCache;
import com.ongres.labs.dyna53.dyna53.TableKeyDefinition;
import com.ongres.labs.dyna53.dynamohttp.exception.DynamoException;
import com.ongres.labs.dyna53.dynamohttp.exception.ResourceNotFoundException;
import com.ongres.labs.dyna53.dynamohttp.exception.ValidationException;
import com.ongres.labs.dyna53.dynamohttp.model.Item;
import com.ongres.labs.dyna53.dynamohttp.request.GetItemRequest;
import com.ongres.labs.dyna53.dynamohttp.request.PutItemRequest;
import com.ongres.labs.dyna53.dynamohttp.request.ScanRequest;
import com.ongres.labs.dyna53.route53.ResourceRecordValue;
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

    private TableDefinition validateItemGetTableDefinition(String tableName, Item item) throws DynamoException {
        var tableDefinitionOptional = tableDefinitionCache.tableDefinition(tableName);
        if(tableDefinitionOptional.isEmpty()) {
            throw new ResourceNotFoundException("Requested resource not found");
        }
        var tableDefinition = tableDefinitionOptional.get();

        // Validate the request contains the hk and rk if applicable
        var hashKey = tableDefinition.getHashKey();
        validateKey(item, hashKey);

        var rangeKey = tableDefinition.getRangeKey();
        if(rangeKey.isPresent()) {
            validateKey(item, rangeKey.get());
        }

        return tableDefinition;
    }

    public void putItem(PutItemRequest putItemRequest) throws DynamoException {
        var dyna53TableName = Dynamo2Route53.toValidRoute53Label(putItemRequest.tableName());
        var item = putItemRequest.item();
        var tableDefinition = validateItemGetTableDefinition(dyna53TableName, item);

        // Insert the item
        var serializedItem = dynamo2Route53.serializeResourceRecord(
                jsonb.toJson(item, Item.class)
        );
        var hashKeyName = tableDefinition.getHashKey().keyName();
        var hkLabel = Dynamo2Route53.hashHK2label(
                item.getAttribute(hashKeyName).get().value()
        );
        try {
            route53Manager.createSingleValuedTXTResource(hkLabel, dyna53TableName, serializedItem);
        } catch (ResourceRecordValue.InvalidValueException e) {
            throw new ValidationException("Item size is too large (max size = 4,000 chars minus escape and formatting)");
        }
    }

    public Item getItem(GetItemRequest getItemRequest) throws DynamoException {
        var dyna53TableName = Dynamo2Route53.toValidRoute53Label(getItemRequest.tableName());
        var keyItem = getItemRequest.key();
        var tableDefinition = validateItemGetTableDefinition(dyna53TableName, keyItem);

        var hashKeyName = tableDefinition.getHashKey().keyName();
        var hkLabel = Dynamo2Route53.hashHK2label(
                keyItem.getAttribute(hashKeyName).get().value()
        );

        return route53Manager.getSingleValuedTXTResource(hkLabel, dyna53TableName)
                .map(serialized -> dynamo2Route53.deserializeResourceRecord(serialized))
                .map(deserialized -> jsonb.fromJson(deserialized, Item.class))
                .orElseThrow(
                        () -> new ResourceNotFoundException("Requested resource not found")
                );
    }

    public Stream<Item> scan(ScanRequest scanRequest) {
        return route53Manager.listTXTRecordsWithLabel(
                scanRequest.tableName(), Dynamo2Route53.ITEM_ROUTE53_LABEL_REGEX
        ).map(value -> dynamo2Route53.deserializeResourceRecord(value))
        .map(value -> jsonb.fromJson(value, Item.class));
    }
}
