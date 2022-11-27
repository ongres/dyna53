/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.route53;


import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.route53.Route53AsyncClient;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;


@ApplicationScoped
public class Route53AsyncClientProvider {
    private static final String DYNA53_ROUTE53_USER_AGENT_PREFIX = "dyna53::Route53";

    @ConfigProperty(name = "route53_aws_profile")
    String route53AWSProfile;

    @Produces
    public Route53AsyncClient provideRoute53AsyncClient() {
        return Route53AsyncClient.builder()
                // Play nice and provide explicit identification for Dyna53 requests to AWS Route 53 services
                .overrideConfiguration(
                        ClientOverrideConfiguration.builder()
                                .putAdvancedOption(SdkAdvancedClientOption.USER_AGENT_PREFIX, DYNA53_ROUTE53_USER_AGENT_PREFIX)
                                .build()
                )
                .credentialsProvider(ProfileCredentialsProvider.create(route53AWSProfile))
                .httpClientBuilder(NettyNioAsyncHttpClient.builder())   // Needed by GraalVM native, reflection issues
                .region(Region.AWS_GLOBAL)
                .build();
    }
}
