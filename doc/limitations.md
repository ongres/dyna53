# Limitations

[AWS Route53 has some limitations](https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/DNSLimitations.html#limits-api-entities-records) that result also on some limitations for Dyna53, like the maximum number of records, values or value length.


## Tables

* Table names:
    * Uppercase letters will be converted to lowercase.
    * Tables names cannot start or end with a hyphen (`-`), and would be removed. Hyphens are allowed in the middle of the table name.
    * A maximum lenght of 63 characters is supported. Requests for table names longer than this will be capped to 63 characters.

## Items
* Item characters should be contained within the extended ASCII 8-bit character set. No UTF-8 characters are supported (sorry, no emojis!).
* Item length is limited to 4,000 characters, including the JSON-like formatting, minus additional characters that may be used to escape characters that require escaping.
* **Tables with hash key primary key**:
    * A maximum of 10,000 items can be stored (unless the quota for the number of records on the Route53 zone is raised). More precisely, three records will be used for the NS and SOA records and table definition, so at most 9,997 items can be stored.
* **Tables with hash key and range key primary key**:
    * A maximum of 4 million records can be stored, with a maximum of 400 different items with the same hash key. Less than 400 items with the same hash key will be stored if their combined length is greater than 32,000 characters (which means that to fit 400 records per hash key records need to be less than 80 characters).


## Implemented Dynamo commands

All of the following commands are implemented with basic behavior, with potentially flags being ignored by the implementation and information missing from the responses:

* Create table.
* Describe table.
* Describe time to live.
* List tables.
* Put item.
* Get item.
* Scan.

However, existing operations work well with usual Dynamo clients like the AWS CLI or Dynobase.


## References

* [DynamoDB API Reference: TableDescription](https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_TableDescription.html).

* [Route53 domain name format](https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/DomainNameFormat.html).
