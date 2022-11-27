# How to run Dyna53

## CLI and configuration

Dyna53 can be either downloaded from the Releases or compiled by yourself. Once the binary is downloaded or generated it can be run calling the binary `dyna53`.

It requires four parameters for configuration, that can be passed by any of the mechanisms supported by [MicroProfile Configuration](https://microprofile.io/microprofile-config/). Most typically it would be either command line arguments or environment variables:

CLI arguments, Java style:
```sh
./dyna53 -Dhosted_zone=XXX -Daccess_key_id=XXX -Dsecret_access_key=XXX -Droute53_aws_profile=XXX
```

Environment variables:
```sh
export hosted_zone=XXX
export access_key_id=XXX
export secret_access_key=XXX
export route53_aws_profile=XXX
./dyna53
```

The configuration variables are the following, and all are compulsory:
* `hosted_zone`: the ID (e.g. `ZXXXXXXXXXXXXXXXU`) of the Route53 DNS zone where data will be stored. It can be public (WARNING: data will become public!) or a private zone.
* `route53_aws_profile`: the name of the configured AWS profile where `dyna53` will be running that has permissions to perform the required operation on Route53.
* `access_key_id` and `secret_access_key`: the credentials used to authenticate the users against Dyna53. Note that these credentials are entirely made up, they don't need to exist on IAM.


## Environment

Dyna53 is a Linux binary that could run anywhere. It could be your laptop, but for production-like performance you probably want to run it on a Lambda or EC2 instance. For a true serverless experience, it should run as a Lambda (with a custom runtime, to support the Linux binary).
