/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dyna53;


import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import java.util.Locale;


@ApplicationScoped
public class Dynamo2Route53 {
    private static final int DNS_LABEL_MAX_LENGTH = 63;

    @Inject
    Jsonb jsonb;

    public static String toValidRoute53Label(String value) {
        var noStartHyphen = value.startsWith("-") ? value.substring(1) : value;
        var length = noStartHyphen.length() > DNS_LABEL_MAX_LENGTH ? DNS_LABEL_MAX_LENGTH : noStartHyphen.length();
        var noStartHyphenCapped = noStartHyphen.substring(0, length);

        var noStartNorEndHyphen = noStartHyphenCapped.charAt(length - 1) == '-' ?
                noStartHyphenCapped.substring(0, length - 1) :
                noStartHyphenCapped;

        return noStartNorEndHyphen.toLowerCase(Locale.US);
    }

    public String serializeTableDefinition(TableDefinition tableDefinition) {
        return serialize(
                jsonb.toJson(tableDefinition)
        );
    }

    public TableDefinition deserializeTableDefinition(String tableName, String serializedTableDefinition) {
        var tableDefinition = jsonb.fromJson(
                deserialize(serializedTableDefinition),
                TableDefinition.class
        );

        tableDefinition.setTableName(tableName);

        return tableDefinition;
    }

    public String serializeResourceRecord(String value) {
        // TODO: escape characters, cut length, split into 255-char chunks...
        return serialize(value);
    }

    private String serialize(String value) {
        // To avoid having to escape double quote in JSON ('"'), we convert to single quote. Better for Route53
        return value.replace('"', '\'');
    }

    private String deserialize(String value) {
        return value.replace('\'', '"');
    }
}
