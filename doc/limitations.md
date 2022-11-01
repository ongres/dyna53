# Limitations

[AWS Route53 has some limitations](https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/DNSLimitations.html#limits-api-entities-records) that result also on some limitations for Dyna53, like the maximum number of records, values or value length.


## Tables

* Table names:
    * Uppercase letters will be converted to lowercase.
    * Tables names cannot start or end with a hyphen (`-`), and would be removed. Hyphens are allowed in the middle of the table name.
    * A maximum lenght of 63 characters is supported. Requests for table names longer than this will be capped to 63 characters.


## References

* [DynamoDB API Reference: TableDescription](https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_TableDescription.html).

* [Route53 domain name format](https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/DomainNameFormat.html).
