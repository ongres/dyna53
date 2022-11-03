/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.route53;


import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.route53.Route53AsyncClient;
import software.amazon.awssdk.services.route53.model.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;


@ApplicationScoped
public class Route53Manager {
    @ConfigProperty(name = "hosted_zone")
    String hostedZone;

    @ConfigProperty(name = "zone_domain_name")
    String zoneDomainName;

    @Inject
    Route53AsyncClient route53AsyncClient;

    public String getSingleValuedResource(String label) {
        var result = route53AsyncClient.listResourceRecordSets(
                ListResourceRecordSetsRequest.builder()
                        .hostedZoneId(hostedZone)
                        .startRecordName(label + "." + zoneDomainName)
                        .maxItems("" + 1)
                        .build()
        ).join();

        if(! result.hasResourceRecordSets()) {
            // TODO: error handling
        }

        return resourceValue2String(
                // TODO: hackish, no error checks
                result.resourceRecordSets().get(0).resourceRecords().get(0).value()
        );
    }

    public void createSingleValuedResource(String label, String value) {
        // TODO: error checking, timeouts, etc
        createAsyncSingleValuedResource(label, value).join();
    }

    private CompletableFuture<ChangeResourceRecordSetsResponse> createAsyncSingleValuedResource(String label, String value) {
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
                                                                                singleValuedResouce(value)
                                                                        )
                                                                        .ttl(1L)
                                                                        .build()
                                                        ).build()
                                        ).build()
                        ).build()
        );
    }

    private ResourceRecord singleValuedResouce(String value) {
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

    /**
     * Route53 resource record TXT values are always quoted. This method unquotes them.
     */
    private String resourceValue2String(String rrValue) {
        return rrValue.substring(1, rrValue.length() - 1);
    }
}
