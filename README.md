# Dyna53: DynamoDB-compatible database backed by Route53


## What is it?

It is a database (with limited functionality). It is in reality a frontend to another database.

The frontend is DynamoDB API compatible. That is, it implements (a limited subset of) the same API that DynamoDB exposes. Therefore it is (should be) compatible with DynamoDB clients and tools. The main goal is to support very basic operations (create table, put item, basic querying capabilities) and be capable of running [YCSB](https://github.com/brianfrankcooper/YCSB) for DynamoDB. Because everybody loves benchmarks!

The backend is [AWS Route 53](https://aws.amazon.com/route53/), a DNS service. This is where data is stored and queried from.


## Where does this ridiculous idea come from?

[Corey Quinn](https://twitter.com/QuinnyPig) once declared that

> Route 53 (Amazon’s managed DNS service) is the only AWS service with a public 100% SLA on the data plane.

> I do declare that Route 53 is in fact a database.

Using DNS "as a database" is not a novel idea, but the concept of running a database on top of Route 53 has not been explored deep enough. There are a couple of related projects: [ten43](https://github.com/tbhb/ten34), which exposes a key-value interface; or [DiggyDB](https://www.npmjs.com/package/diggydb-nodejs), which exposes a document-like interface for JS applications.

However none of them exposes a well-known API like serverless' DynamoDB, which makes it very convenient supporting existing tools and code (including benchmarks!). This is the main driver behind this project.


## Limitations

[AWS Route53 has some limitations](https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/DNSLimitations.html#limits-api-entities-records) that result also on some limitations for Dyna53, like the maximum number of records, values or value length.


## Disclaimer

This is a toy, side-project. It is not intended to be used in production. Use at your own risk.