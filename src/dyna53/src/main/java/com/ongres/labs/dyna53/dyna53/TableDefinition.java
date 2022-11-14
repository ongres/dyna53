/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dyna53;


public record TableDefinition (
    String tableName,
    TableKeysDefinition keysDefinition
) {
}
