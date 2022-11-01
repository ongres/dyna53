/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.route53;


import com.ongres.labs.dyna53.dyna53.TableDefinition;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.route53.Route53AsyncClient;
import software.amazon.awssdk.services.route53.model.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import java.util.concurrent.CompletableFuture;


@ApplicationScoped
public class Route53Manager {
    @ConfigProperty(name = "hosted_zone")
    String hostedZone;

    @ConfigProperty(name = "zone_domain_name")
    String zoneDomainName;

    @Inject
    Route53AsyncClient route53AsyncClient;

    @Inject
    Jsonb jsonb;

    public String createTable(TableDefinition tableDefinition) {

        var response = createSingleValuedResource(
                tableDefinition.tableName(),
                // To avoid having to escape double quote in JSON ('"'), we convert to single quote. Better for Route53
                // Only applies to table definitions stored in Route53 records
                jsonb.toJson(tableDefinition).replace('"', '\'')
        );

        response.join();

        return "TODO";
    }

    private CompletableFuture<ChangeResourceRecordSetsResponse> createSingleValuedResource(String label, String value) {
        return route53AsyncClient.changeResourceRecordSets(
                ChangeResourceRecordSetsRequest.builder()
                        .hostedZoneId(hostedZone)
                        .changeBatch(
                                ChangeBatch.builder()
                                        .changes(
                                                Change.builder()
                                                        .action(ChangeAction.CREATE)
                                                        .resourceRecordSet(
                                                                ResourceRecordSet.builder()
                                                                        .name(label + "." + zoneDomainName)
                                                                        .type(RRType.TXT)
                                                                        .resourceRecords(
                                                                                singleValuedRR(value)
                                                                        )
                                                                        .ttl(1L)
                                                                        .build()
                                                        ).build()
                                        ).build()
                        ).build()
        );
    }

    private ResourceRecord singleValuedRR(String value) {
        var stringBuilder = new StringBuilder()
                .append("\"")
                // TODO: escape needed characters
                .append(value)
                .append("\"")
        ;

        return ResourceRecord.builder()
                // TODO: if value.length() > 255 it needs to be split into multiple strings
                .value(stringBuilder.toString())
                .build();
    }
}
