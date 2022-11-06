/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dyna53;


import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;


@ApplicationScoped
public class Dynamo2Route53 {
    private static final int DNS_LABEL_MAX_LENGTH = 63;

    private static final HexFormat HEX_FORMAT_LOWERCASE_NO_SEPARATORS_PREFIXES = HexFormat.of();

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

    /*
     * The hash key of an item will be used as a label as part of the name of the Route53 RecordSet where the item
     * will be stored. Due to the length and character set restrictions of labels in domain names in general and
     * Route53 in particular (specially the conversion to lower case!) it's best to generate a derivative of the hash
     * key that will fit into the limitations of the domain label. This is the role of this function.
     *
     * This function's implementation computes the SHA-224 hash of the hash key, which produces 56 hexadecimal
     * characters and use it as a label (space could be reduced if mapped to a wider character set; but it is also
     * a goal to make generation of these labels from outside dyna53 as easy as possible, like from the shell).
     */
    public static String hashHK2label(String hkValue) {
        try {
            var messageDigest = MessageDigest.getInstance("SHA-224");
            var hashedBytes = messageDigest.digest(hkValue.getBytes(StandardCharsets.UTF_8));

            return HEX_FORMAT_LOWERCASE_NO_SEPARATORS_PREFIXES.formatHex(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("dyna53 requires a JRE with SHA-224 support", e);
        }
    }
}
