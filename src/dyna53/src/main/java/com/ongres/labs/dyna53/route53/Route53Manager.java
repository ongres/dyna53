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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;


@ApplicationScoped
public class Route53Manager {
    // SRV records will be used as (effectively) simple text records by ignoring the first three integer fields
    private static final String SRV_DUMMY_PREFIX = "0 53 0 ";

    // If records are searched by lexicographical order, there can be no records before this one
    // Note that the '\0' character needs to be escaped as '\000'
    private static final String FIRST_LETTER_FIRST_POSSIBLE_LEXICOGRAPHICAL_DOMAIN_NAME_ROUTE53_ESCAPED = "\\000";

    @ConfigProperty(name = "hosted_zone")
    String hostedZone;

    @ConfigProperty(name = "zone_domain_name")
    String zoneDomainName;

    @Inject
    Route53AsyncClient route53AsyncClient;

    private Optional<String> getSingleValuedResource(String label, RRType rrType) {
        var result = route53AsyncClient.listResourceRecordSets(
                ListResourceRecordSetsRequest.builder()
                        .hostedZoneId(hostedZone)
                        .startRecordName(label + "." + zoneDomainName)
                        .startRecordType(rrType)
                        .maxItems("" + 1)
                        .build()
        ).join();

        if(! result.hasResourceRecordSets()) {
            return Optional.empty();
        }
        var resourceRecordsSetList = result.resourceRecordSets();
        if(resourceRecordsSetList.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(
                // TODO: hackish, no error checks
                resourceRecordsSetList.get(0).resourceRecords().get(0).value()
        );
    }

    public Optional<String> getSingleValuedTXTResource(String label) {
        return getSingleValuedResource(label, RRType.TXT).map(r -> txtResourceValue2String(r));
    }

    public Optional<String> getSingleValuedSRVResource(String label) {
        return getSingleValuedResource(label, RRType.SRV).map(r -> srvResourceValue2String(r));
    }

    // TODO: error checking, timeouts, etc
    private CompletableFuture<ChangeResourceRecordSetsResponse> createAsyncResourceRecord(
            String label, RRType rrType, ResourceRecord resourceRecord
    ) {
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
                                                                        .type(rrType)
                                                                        .resourceRecords(resourceRecord)
                                                                        .ttl(1L)
                                                                        .build()
                                                        ).build()
                                        ).build()
                        ).build()
        );
    }

    private CompletableFuture<ChangeResourceRecordSetsResponse> createSingleValuedTXTResource(
            String label, String value
    ) {
        return createAsyncResourceRecord(label, RRType.TXT, singleValuedResource(value));
    }

    public void createSingleValuedTXTResource(String subLabel, String label, String value) {
        createSingleValuedTXTResource(subLabel + "." + label, value).join();
    }

    public void createDummySRVResource(String label, String value) {
        createAsyncResourceRecord(
                label, RRType.SRV, value2DummySRVResource(value)
        ).join();
    }

    private ResourceRecord singleValuedResource(String value) {
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
    private String txtResourceValue2String(String rrValue) {
        return rrValue.substring(1, rrValue.length() - 1);
    }

    private ResourceRecord value2DummySRVResource(String value) {
        return ResourceRecord.builder()
                .value(SRV_DUMMY_PREFIX + value)
                .build();
    }

    private String srvResourceValue2String(String rrValue) {
        return rrValue.substring(SRV_DUMMY_PREFIX.length());
    }

    public Stream<String> listSRVRecordsLabel() {
        var result = route53AsyncClient.listResourceRecordSets(
                ListResourceRecordSetsRequest.builder()
                        .hostedZoneId(hostedZone)
                        .startRecordType(RRType.SRV)
                        // Surprisingly, if we don't include .startRecordName() we get a 400 - InvalidInputException
                        .startRecordName(FIRST_LETTER_FIRST_POSSIBLE_LEXICOGRAPHICAL_DOMAIN_NAME_ROUTE53_ESCAPED)
                        .build()
        ).join();

        return result.resourceRecordSets().stream()
                // Exclude non-dyna53 SRV records
                .filter(resourceRecordSet ->
                        resourceRecordSet.resourceRecords().stream()
                                .allMatch(resourceRecord -> resourceRecord.value().startsWith(SRV_DUMMY_PREFIX))
                )
                .map(resourceRecordSet -> route53FullLabel2Label(resourceRecordSet.name()));
    }

    private String route53FullLabel2Label(String value) {
        return value.substring(0, value.length() - ".".length() - zoneDomainName.length()  - 1);
    }
}
