/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dyna53;


import java.util.Locale;

public class Dynamo2Route53 {
    private static final int DNS_LABEL_MAX_LENGTH = 63;

    public static String mapTableName(String tableName) {
        var noStartHyphen = tableName.startsWith("-") ? tableName.substring(1) : tableName;
        var length = noStartHyphen.length() > DNS_LABEL_MAX_LENGTH ? DNS_LABEL_MAX_LENGTH : noStartHyphen.length();
        var noStartHyphenCapped = noStartHyphen.substring(0, length);

        var noStartNorEndHyphen = noStartHyphenCapped.charAt(length - 1) == '-' ?
                noStartHyphenCapped.substring(0, length - 1) :
                noStartHyphenCapped;

        return noStartNorEndHyphen.toLowerCase(Locale.US);
    }
}
