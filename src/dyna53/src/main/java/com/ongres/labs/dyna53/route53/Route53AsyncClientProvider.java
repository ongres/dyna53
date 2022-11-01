/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.route53;


import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.route53.Route53AsyncClient;

import javax.enterprise.inject.Produces;


public class Route53AsyncClientProvider {
    @Produces
    public Route53AsyncClient provideRoute53AsyncClient() {
        return Route53AsyncClient.builder()
                .credentialsProvider(ProfileCredentialsProvider.create("dyna53"))
                .httpClientBuilder(NettyNioAsyncHttpClient.builder())   // Needed by GraalVM native, reflection issues
                .region(Region.AWS_GLOBAL)
                .build();
    }
}
