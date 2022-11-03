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

    public static String mapTableName(String tableName) {
        var noStartHyphen = tableName.startsWith("-") ? tableName.substring(1) : tableName;
        var length = noStartHyphen.length() > DNS_LABEL_MAX_LENGTH ? DNS_LABEL_MAX_LENGTH : noStartHyphen.length();
        var noStartHyphenCapped = noStartHyphen.substring(0, length);

        var noStartNorEndHyphen = noStartHyphenCapped.charAt(length - 1) == '-' ?
                noStartHyphenCapped.substring(0, length - 1) :
                noStartHyphenCapped;

        return noStartNorEndHyphen.toLowerCase(Locale.US);
    }

    public String serializeTableDefinition(TableDefinition tableDefinition) {
        // To avoid having to escape double quote in JSON ('"'), we convert to single quote. Better for Route53
        // Only applies to table definitions stored in Route53 records
        return jsonb.toJson(tableDefinition).replace('"', '\'');
    }

    public TableDefinition deserializeTableDefinition(String serializedTableDefinition) {
        var asValidJson = serializedTableDefinition.replace('\'', '"');
        var tableDefinition = jsonb.fromJson(asValidJson, TableDefinition.class);

        return tableDefinition;
    }
}
