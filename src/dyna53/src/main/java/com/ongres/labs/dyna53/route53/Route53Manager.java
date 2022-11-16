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
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;


@ApplicationScoped
public class Route53Manager {
    // If records are searched by lexicographical order, there can be no records before this one
    // Note that the '\0' character needs to be escaped as '\000'
    private static final String FIRST_LETTER_FIRST_POSSIBLE_LEXICOGRAPHICAL_DOMAIN_NAME_ROUTE53_ESCAPED = "\\000";

    private static final int ROUTE53_API_CALLS_TIMEOUT_SECONDS = 1;

    @ConfigProperty(name = "hosted_zone")
    String hostedZone;

    @ConfigProperty(name = "zone_domain_name")
    String zoneDomainName;

    @Inject
    Route53AsyncClient route53AsyncClient;

    private String recordNameDotEnded(String label) {
        return label + "." + zoneDomainName + ".";
    }

    private String labelFrom(String subLabel, String label) {
        return subLabel + "." + label;
    }

    private String route53FullLabel2Label(String value) {
        return value.substring(0, value.length() - ("." + zoneDomainName).length() - 1);
    }

    private CompletableFuture<ChangeResourceRecordSetsResponse> actionAsyncResourceRecord(
            ChangeAction changeAction, String label, RRType rrType, ResourceRecord resourceRecord
    ) {
        return route53AsyncClient.changeResourceRecordSets(
                ChangeResourceRecordSetsRequest.builder()
                        .hostedZoneId(hostedZone)
                        .changeBatch(
                                ChangeBatch.builder()
                                        .changes(
                                                Change.builder()
                                                        .action(changeAction)
                                                        .resourceRecordSet(
                                                                ResourceRecordSet.builder()
                                                                        .name(recordNameDotEnded(label))
                                                                        .type(rrType)
                                                                        .resourceRecords(resourceRecord)
                                                                        .ttl(1L)
                                                                        .build()
                                                        ).build()
                                        ).build()
                        ).build()
        );
    }

    private void actionResourceRecord(
            ChangeAction changeAction, String label, RRType rrType, ResourceRecord resourceRecord
    ) throws TimeoutException, ResourceRecordException {
        var resultThrowable = actionAsyncResourceRecord(
                changeAction, label, rrType, resourceRecord
        )
                .orTimeout(ROUTE53_API_CALLS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .handle(
                        (response, throwable) -> Optional.ofNullable(throwable).map(t -> t.getCause())
                )
                .join();

        if(resultThrowable.isPresent()) {
            if(resultThrowable.get() instanceof InvalidChangeBatchException) {
                // Potentially it could be other causes, but let's assume here it's the record already exist
                throw ResourceRecordException.valueAlreadyExistsException();
            } else if(resultThrowable.get() instanceof TimeoutException) {
                throw new TimeoutException();
            }
        }
    }

    private ResourceRecord singleValuedResource(String value) throws InvalidValueException {
        var valueRoute53Formatted = ResourceRecordValue.toRoute53Value(value);

        return ResourceRecord.builder()
                .value(valueRoute53Formatted)
                .build();
    }

    private void actionSingleValuedTXTResource(ChangeAction changeAction, String subLabel, String label, String value)
    throws InvalidValueException, ResourceRecordException, TimeoutException {
        actionResourceRecord(
                changeAction,
                labelFrom(subLabel, label),
                RRType.TXT,
                singleValuedResource(value)
        );
    }

    public void upsertSingleValuedTXTResource(String subLabel, String label, String value)
    throws InvalidValueException, ResourceRecordException, TimeoutException {
        actionSingleValuedTXTResource(ChangeAction.UPSERT, subLabel, label, value);
    }

    public void createSingleValuedTXTResource(String subLabel, String label, String value)
            throws InvalidValueException, ResourceRecordException, TimeoutException {
        actionSingleValuedTXTResource(ChangeAction.CREATE, subLabel, label, value);
    }

    private Optional<String> getSingleValuedResource(String label, RRType rrType) {
        var result = route53AsyncClient.listResourceRecordSets(
                ListResourceRecordSetsRequest.builder()
                        .hostedZoneId(hostedZone)
                        .startRecordName(recordNameDotEnded(label))
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

        var resourceRecordSet = resourceRecordsSetList.get(0);

        return resourceRecordSet.name().isEmpty() || (! resourceRecordSet.name().equals(recordNameDotEnded(label))) ?
                Optional.empty() :
                Optional.of(
                        // TODO: hackish, no error checks
                        resourceRecordSet.resourceRecords().get(0).value()
                );
    }

    /**
     * Route53 resource record TXT values are chunked, escaped, etc. This method reconstructs back the original string
     */
    private String txtResourceValue2String(String rrValue) {
        return ResourceRecordValue.fromRoute53Value(rrValue);
    }

    public Optional<String> getSingleValuedTXTResource(String label, String subLabel) {
        return getSingleValuedResource(
                labelFrom(label, subLabel), RRType.TXT
        )
                .map(r -> txtResourceValue2String(r));
    }

    public void createSRVResource(String label, String value, int priority, int weight, int port)
    throws TimeoutException, ResourceRecordException {
        actionResourceRecord(ChangeAction.CREATE, label, RRType.SRV, value2SRVResource(priority, weight, port, value));
    }

    private String srvResourceValue2String(int priority, int weight, int port, String rrValue) {
        return rrValue.substring(generateSRVValuePrefix(priority, weight, port).length());
    }

    public Optional<String> getSingleValuedSRVResource(int priority, int weight, int port, String label) {
        return getSingleValuedResource(label, RRType.SRV).map(r -> srvResourceValue2String(priority, weight, port, r));
    }

    private String generateSRVValuePrefix(int priority, int weight, int port) {
        return priority + " " + weight + " " + port + " ";
    }

    private ResourceRecord value2SRVResource(int priority, int weight, int port, String value) {
        return ResourceRecord.builder()
                .value(
                        generateSRVValuePrefix(priority, weight, port) + value
                )
                .build();
    }

    private Stream<ResourceRecordSet> listAllRecords(RRType rrType) {
        return route53AsyncClient.listResourceRecordSets(
                ListResourceRecordSetsRequest.builder()
                        .hostedZoneId(hostedZone)
                        .startRecordType(rrType)
                        // Surprisingly, if we don't include .startRecordName() we get a 400 - InvalidInputException
                        .startRecordName(FIRST_LETTER_FIRST_POSSIBLE_LEXICOGRAPHICAL_DOMAIN_NAME_ROUTE53_ESCAPED)
                        .build()
        ).join()
        .resourceRecordSets().stream();
    }

    public Stream<String> listSRVRecordsLabel(int priority, int weight, int port) {
        return listAllRecords(RRType.SRV)
                // Exclude non-dyna53 SRV records
                .filter(resourceRecordSet ->
                        resourceRecordSet.resourceRecords().stream()
                                .allMatch(resourceRecord -> resourceRecord.value().startsWith(
                                        generateSRVValuePrefix(priority, weight, port)
                                ))
                )
                .map(resourceRecordSet -> route53FullLabel2Label(resourceRecordSet.name()));
    }

    public Stream<String> listTXTRecordsWithLabel(String label, String namePrefixRegex) {
        return listAllRecords(RRType.TXT)
                .filter(resourceRecordSet ->
                        resourceRecordSet.name().endsWith("." + label + "." + zoneDomainName + ".")
                        && resourceRecordSet.name().split("\\.")[0].matches(namePrefixRegex)
                ).flatMap(resourceRecordSet -> resourceRecordSet.resourceRecords().stream())
                .map(resourceRecord -> txtResourceValue2String(resourceRecord.value()))
                ;
    }
}
