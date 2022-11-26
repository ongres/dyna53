/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.route53;


import com.ongres.labs.dyna53.route53.exception.InvalidValueException;
import com.ongres.labs.dyna53.route53.exception.ResourceRecordException;
import com.ongres.labs.dyna53.route53.exception.Route53Exception;
import com.ongres.labs.dyna53.route53.exception.TimeoutException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.route53.Route53AsyncClient;
import software.amazon.awssdk.services.route53.model.*;

import javax.annotation.PostConstruct;
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

    private static final int MAX_VALUES_PER_RESOURCE_RECORD = 400;

    private static final int MAX_RECORDS_HOSTED_ZONE = 10_000;

    // Max number of items on a table: max records minus SOA and NS for the zone minus a single table definition SRV
    private static final int MAX_ITEMS_HOSTED_ZONE = MAX_RECORDS_HOSTED_ZONE - 2 - 1;

    private static final Logger LOGGER = LoggerFactory.getLogger(Route53Manager.class);

    @ConfigProperty(name = "hosted_zone")
    String hostedZone;

    String zoneDomainName;

    @Inject
    Route53AsyncClient route53AsyncClient;

    @PostConstruct
    void getZoneDomainNameFromHostedZone() {
        var getHostedZoneRequest = GetHostedZoneRequest.builder()
                .id(hostedZone)
                .build();

        route53AsyncClient.getHostedZone(getHostedZoneRequest)
                .orTimeout(ROUTE53_API_CALLS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete(
                        (response, throwable) -> {
                            if(null == response) {
                                throw new RuntimeException("Invalid hosted zone id " + hostedZone + " or error querying it");
                            }

                            zoneDomainName = response.hostedZone().name();
                            LOGGER.info("Setting zone domain name to " + zoneDomainName);
                        }
                )
                .join();
    }

    private String recordNameDotEnded(String label) {
        return label + "." + zoneDomainName;
    }

    private String labelFrom(String subLabel, String label) {
        return subLabel + "." + label;
    }

    private String route53FullLabel2Label(String value) {
        return value.substring(0, value.length() - ("." + zoneDomainName).length());
    }

    private CompletableFuture<ChangeResourceRecordSetsResponse> actionAsyncResourceRecord(
            ChangeAction changeAction, String label, RRType rrType, ResourceRecord... resourceRecords
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
                                                                        .resourceRecords(resourceRecords)
                                                                        .ttl(1L)
                                                                        .build()
                                                        ).build()
                                        ).build()
                        ).build()
        );
    }

    private void actionResourceRecord(
            ChangeAction changeAction, String label, RRType rrType, ResourceRecord... resourceRecords
    ) throws Route53Exception {
        var resultThrowable = actionAsyncResourceRecord(
                changeAction, label, rrType, resourceRecords
        )
                .orTimeout(ROUTE53_API_CALLS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .handle(
                        (response, throwable) -> Optional.ofNullable(throwable).map(t -> t.getCause())
                )
                .join();

        if(resultThrowable.isPresent()) {
            if(resultThrowable.get() instanceof InvalidChangeBatchException e && e.statusCode() == 400) {
                if(e.getMessage().matches(".* but it already exists.*")) {
                    throw ResourceRecordException.valueAlreadyExistsException();
                }
                throw new Route53Exception(e.getMessage());
            } else if(resultThrowable.get() instanceof TimeoutException) {
                throw new TimeoutException();
            }
        }
    }

    private ResourceRecord generateResourceRecord(String value) throws InvalidValueException {
        return ResourceRecord.builder()
                .value(
                    ResourceRecordValue.toRoute53Value(value)
                )
                .build();
    }

    private ResourceRecord[] generateResourceRecords(String... values) throws InvalidValueException {
        var resourceRecords = new ResourceRecord[values.length];
        for(int i = 0; i < values.length; i++) {
            resourceRecords[i] = generateResourceRecord(values[i]);
        }

        return resourceRecords;
    }

    private void actionMultiValuedTXTResource(ChangeAction changeAction, String subLabel, String label, String[] values)
    throws Route53Exception {
        actionResourceRecord(
                changeAction,
                labelFrom(subLabel, label),
                RRType.TXT,
                generateResourceRecords(values)
        );
    }

    public void upsertMultiValuedTXTResource(String subLabel, String label, String... values) throws Route53Exception {
        if(values.length > MAX_VALUES_PER_RESOURCE_RECORD) {
            throw new Route53Exception(
                    "Resource Records cannot contain more than " + MAX_VALUES_PER_RESOURCE_RECORD + " values"
            );
        }

        actionMultiValuedTXTResource(ChangeAction.UPSERT, subLabel, label, values);
    }

    private Stream<String> getResourceRecordValues(String label, RRType rrType) {
        var result = route53AsyncClient.listResourceRecordSets(
                ListResourceRecordSetsRequest.builder()
                        .hostedZoneId(hostedZone)
                        .startRecordName(recordNameDotEnded(label))
                        .startRecordType(rrType)
                        .maxItems("" + 1)
                        .build()
        ).join();

        if(! result.hasResourceRecordSets()) {
            return Stream.empty();
        }
        var resourceRecordsSetList = result.resourceRecordSets();
        if(resourceRecordsSetList.isEmpty()) {
            return Stream.empty();
        }

        var resourceRecordSet = resourceRecordsSetList.get(0);

        return resourceRecordSet.name().isEmpty() || (! resourceRecordSet.name().equals(recordNameDotEnded(label))) ?
                Stream.empty()
                : resourceRecordSet.resourceRecords().stream().map(rr -> rr.value())
                ;
    }

    private Optional<String> getSingleValuedResource(String label, RRType rrType) {
        return getResourceRecordValues(label, rrType)
                .findFirst();
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

    public Stream<String> getMultiValuedTXTResource(String label, String subLabel) {
        return getResourceRecordValues(
                labelFrom(label, subLabel), RRType.TXT
        )
                .map(r -> txtResourceValue2String(r));
    }

    public void createSRVResource(String label, String value, int priority, int weight, int port)
    throws Route53Exception {
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
                        .maxItems("" + MAX_ITEMS_HOSTED_ZONE)
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
                        resourceRecordSet.name().endsWith("." + label + "." + zoneDomainName)
                        && resourceRecordSet.name().split("\\.")[0].matches(namePrefixRegex)
                ).flatMap(resourceRecordSet -> resourceRecordSet.resourceRecords().stream())
                .map(resourceRecord -> txtResourceValue2String(resourceRecord.value()))
                ;
    }
}
