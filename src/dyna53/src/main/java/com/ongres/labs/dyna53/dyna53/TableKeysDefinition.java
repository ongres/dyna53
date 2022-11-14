/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dyna53;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;


public record TableKeysDefinition(
    @JsonProperty("hk") TableKeyDefinition hashKey,
    @JsonProperty("rk") Optional<TableKeyDefinition> rangeKey
) {
}
