/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.route53.exception;


import com.ongres.labs.dyna53.route53.ResourceRecordValue;

public class InvalidValueException extends Route53Exception {
    public InvalidValueException(String message) {
        super(message);
    }

    public static InvalidValueException valueTooLongException() {
        return new InvalidValueException(
                "Item size is too large (max size = "
                        + ResourceRecordValue.TXT_VALUE_MAX_LENGTH_CHARS
                        + " chars minus escape and formatting)"
        );
    }
}
