/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.route53;


/* A class to encapsulates the caveats of writing/reading a String to a Route53 record.
 * @see https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/ResourceRecordTypes.html#TXTFormat for authoritative
 * information.
 *
 * It's very odd that some ASCII chars need to be escaped, expanding from 1 to 4 chars. This increased size *counts*
 * towards the maximum record value length (4,000), but *does not count* on the maximum chunk size of 255 chars into
 * which longer strings need to be split.
 */
public class ResourceRecordValue {
    public static final int TXT_VALUE_MAX_LENGTH_CHARS = 4_000;
    public static final int TXT_VALUE_MAX_CHENK_LENGTH = 255;

    // A number higher than the total number of characters that will be wasted due to string chunking
    private static final int MAX_SIZE_SEPARATOR_STRINGS = (
            (TXT_VALUE_MAX_LENGTH_CHARS / TXT_VALUE_MAX_CHENK_LENGTH) + 1
        ) * "\" \"".length();

    public static class InvalidValueException extends Exception {
        public InvalidValueException(String message) {
            super(message);
        }

        public static InvalidValueException valueTooLongException() {
            return new InvalidValueException("Value too long, max length = " + TXT_VALUE_MAX_LENGTH_CHARS + " chars");
        }
    }

    public static String toRoute53Value(String value) throws InvalidValueException {
        if(value.length() > TXT_VALUE_MAX_LENGTH_CHARS) {
            throw InvalidValueException.valueTooLongException();
        }

        var chunkedValue = valueToChunks(value);
        var stringBuilder = new StringBuilder(
                // In the worst case a string will grow in size by a factor of 4:1 plus some additional chars
                Math.max(value.length() * 4 + MAX_SIZE_SEPARATOR_STRINGS, TXT_VALUE_MAX_LENGTH_CHARS)
        );
        for(int i = 0; i < chunkedValue.length; i++) {
            stringBuilder.append("\"");
            validateEscapeValue(chunkedValue[i], stringBuilder);
            stringBuilder.append("\"");
            if(i < (chunkedValue.length) - 1) {
                stringBuilder.append(" ");
            }
            // Surprisingly, the 4,000 chars limit is after all processing, so need to keep checking all the time
            if(stringBuilder.length() > TXT_VALUE_MAX_LENGTH_CHARS) {
                throw InvalidValueException.valueTooLongException();
            }
        }

        return stringBuilder.toString();
    }

    private static String[] valueToChunksByChunkSize(String value, int chunkSize) {
        if(value.length() <= chunkSize) {
            return new String[] { value };
        }

        var length = value.length();
        var nChunks = (length / chunkSize) + (length % chunkSize == 0 ? 0: 1);
        var values = new String[nChunks];
        for(int i = 0; i < nChunks; i++) {
            values[i] = value.substring(i * chunkSize, Math.min((i + 1) * chunkSize, length));
        }

        return values;
    }

    private static String[] valueToChunks(String value) {
        return valueToChunksByChunkSize(value, TXT_VALUE_MAX_CHENK_LENGTH);
    }

    private static void validateEscapeValue(String value, StringBuilder stringBuilder) throws InvalidValueException {
        for(var c : value.toCharArray()) {
            var i = (int) c;
            if(i > 255) {
                throw new InvalidValueException("Only (extended) ASCII characters are supported");
            }

            if(i < 32 || i >= 127) {
                stringBuilder.append("\\").append(Integer.toOctalString(i));
            } else {
                if(i == ((int) '"')) {
                    stringBuilder.append("\\");
                }
                stringBuilder.append(((char) i));
            }
        }
    }

    private static boolean charIsOctalDigit(char c) {
        return c >= '0' && c <= '7';
    }

    private static int deserializeEscapeSequence(char[] chars, int pos, StringBuilder stringBuilder) {
        int i = pos;
        if(i == chars.length) {
            return chars.length;
        }

        if(chars[i] == '"') {
            stringBuilder.append('"');
            return i + 1;
        } else if(charIsOctalDigit(chars[i])) {
            if(i + 2 >= chars.length) {
                return chars.length;
            }
            if(! (charIsOctalDigit(chars[i+1]) && charIsOctalDigit(chars[i+2]))) {
                return i + 3;   // Invalid escape sequence, moving on
            }
            int charValue = ((chars[i] - '0') << 6) + ((chars[i+1] - '0') << 3) + ((chars[i+2] - '0'));
            stringBuilder.append(
                    ((char) charValue)
            );
            return i + 3;
        } else {
            // Shouldn't happen, invalid escape sequence
            return i + 1;
        }
    }

    private static int deserializeChunkValue(char[] chars, int pos, StringBuilder stringBuilder) {
        int i = pos;

        do {
            if(chars[i] == '\\' && i < (chars.length - 1)) {
                i = deserializeEscapeSequence(chars, ++i, stringBuilder);
            } else if(chars[i] == '"') {
                return i + 1;
            } else {
                stringBuilder.append(chars[i]);
                i++;
            }
        } while(i < chars.length);

        return chars.length;
    }

    public static String fromRoute53Value(String value) {
        var stringBuilder = new StringBuilder(value.length());

        char[] chars = value.toCharArray();
        int i = 0;
        do {
            if(chars[i] == '"' && i < (chars.length - 1)) {
                i = deserializeChunkValue(chars, ++i, stringBuilder);
            } else {
                i++;
            }
        } while(i < value.length());

        return stringBuilder.toString();
    }
}
