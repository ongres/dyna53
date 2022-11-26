/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dyna53.processor;


import com.ongres.labs.dyna53.dyna53.Dynamo2Route53;
import com.ongres.labs.dyna53.dyna53.TableDefinition;
import com.ongres.labs.dyna53.dyna53.TableDefinitionCache;
import com.ongres.labs.dyna53.dyna53.TableKeyDefinition;
import com.ongres.labs.dyna53.dyna53.jackson.Dyna53ObjectMapper;
import com.ongres.labs.dyna53.dynamohttp.exception.*;
import com.ongres.labs.dyna53.dynamohttp.model.Item;
import com.ongres.labs.dyna53.dynamohttp.request.GetItemRequest;
import com.ongres.labs.dyna53.dynamohttp.request.PutItemRequest;
import com.ongres.labs.dyna53.dynamohttp.request.ScanRequest;
import com.ongres.labs.dyna53.route53.Route53Manager;
import com.ongres.labs.dyna53.route53.exception.InvalidValueException;
import com.ongres.labs.dyna53.route53.exception.ResourceRecordException;
import com.ongres.labs.dyna53.route53.exception.Route53Exception;
import com.ongres.labs.dyna53.route53.exception.TimeoutException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;


@ApplicationScoped
public class ItemProcessor {
    @Inject
    Route53Manager route53Manager;

    @Inject
    TableDefinitionCache tableDefinitionCache;

    @Inject
    Dyna53ObjectMapper dyna53ObjectMapper;

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
        var tableKeysDefinition = tableDefinition.keysDefinition();

        // Validate the request contains the hk and rk if applicable
        var hashKey = tableKeysDefinition.hashKey();
        validateKey(item, hashKey);

        var rangeKey = tableKeysDefinition.rangeKey();
        if(rangeKey.isPresent()) {
            validateKey(item, rangeKey.get());
        }

        return tableDefinition;
    }

    public void putItem(PutItemRequest putItemRequest) throws DynamoException {
        var dyna53TableName = Dynamo2Route53.toValidRoute53Label(putItemRequest.tableName());
        var item = putItemRequest.item();
        var tableDefinition = validateItemGetTableDefinition(dyna53TableName, item);
        var hashKeyName = tableDefinition.keysDefinition().hashKey().keyName();
        var hkLabel = Dynamo2Route53.hashHK2label(
                item.getAttribute(hashKeyName).get().value()
        );

        Stream<Item> itemStream;
        if(tableDefinition.hasRangeKey()) {
            var rangeKeyName = tableDefinition.keysDefinition().rangeKey().get().keyName();

            // Read all existing records, sort them and add the new one, then write it all
            var orderedItems = new TreeMap<String,Item>();
            getItemsHashAndRangeKey(hkLabel, dyna53TableName)
                    .forEach(it -> orderedItems.put(it.getAttribute(rangeKeyName).get().value(), it));
            orderedItems.put(item.getAttribute(rangeKeyName).get().value(), item);

            itemStream = orderedItems.entrySet().stream()
                    .map(entry -> entry.getValue());
        } else {
            itemStream = Stream.of(item);
        }

        var jsonItems = itemStream
                .map(it -> dyna53ObjectMapper.writeValueAsStringRuntimeException(it))
                .toArray(String[]::new);
        try {
            route53Manager.upsertMultiValuedTXTResource(hkLabel, dyna53TableName, jsonItems);
            //TODO: call create...TXTResource() instead when Conditional expression on item existing is supported
        } catch (InvalidValueException e) {
            throw new ValidationException(e.getMessage());
        } catch (ResourceRecordException e) {
            // TODO: ignored for now. Will be useful to implement ConditionalExpression on item existing, for example
        } catch (TimeoutException e) {
            // User can retry, so let's simulate a provision exceeded exception:
            throw new ProvisionedThroughputExceededException("Operation is retryable");
        } catch (Route53Exception e) {
            // Generic one, abort with an error
            throw new InternalErrorException("Internal error", e);
        }
    }

    private Optional<Item> getItemHashKey(String hkLabel, String dyna53TableName) {
        return route53Manager.getSingleValuedTXTResource(hkLabel, dyna53TableName)
                .map(deserialized -> dyna53ObjectMapper.readValueRuntimeException(deserialized, Item.class));
    }

    private Stream<Item> getItemsHashAndRangeKey(String hkLabel, String dyna53TableName) {
        return route53Manager.getMultiValuedTXTResource(hkLabel, dyna53TableName)
                .map(deserialized -> dyna53ObjectMapper.readValueRuntimeException(deserialized, Item.class));
    }

    public Item getItem(GetItemRequest getItemRequest) throws DynamoException {
        var dyna53TableName = Dynamo2Route53.toValidRoute53Label(getItemRequest.tableName());
        var keyItem = getItemRequest.key();
        var tableDefinition = validateItemGetTableDefinition(dyna53TableName, keyItem);
        var keysDefinition = tableDefinition.keysDefinition();

        var hashKeyName = keysDefinition.hashKey().keyName();
        var hkLabel = Dynamo2Route53.hashHK2label(
                keyItem.getAttribute(hashKeyName).get().value()
        );

        Optional<Item> optionalItem;
        if(! tableDefinition.hasRangeKey()) {
            optionalItem = getItemHashKey(hkLabel, dyna53TableName);
        } else {
            var rangeKeyName = keysDefinition.rangeKey().get().keyName();
            optionalItem = getItemsHashAndRangeKey(hkLabel, dyna53TableName)
                    .filter(item ->
                            item.getAttribute(rangeKeyName).get().value().equals(
                                    keyItem.getAttribute(rangeKeyName).get().value()
                            )
                    )
                    .findFirst();
        }

        return optionalItem.orElseThrow(
                () -> new ResourceNotFoundException("Requested resource not found")
        );
    }

    public Stream<Item> scan(ScanRequest scanRequest) {
        return route53Manager.listTXTRecordsWithLabel(
                scanRequest.tableName(), Dynamo2Route53.ITEM_ROUTE53_LABEL_REGEX
        )
        .map(value -> dyna53ObjectMapper.readValueRuntimeException(value, Item.class));
    }
}
