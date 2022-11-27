/*
 * Copyright (C) 2022 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0
 */


package com.ongres.labs.dyna53.dynamohttp;


import com.ongres.labs.dyna53.dynamohttp.model.ErrorResponse;
import com.ongres.labs.dyna53.dynamohttp.model.ErrorResponseType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;


@ApplicationScoped
public class SignatureV4Validator {
    @ConfigProperty(name = "access_key_id")
    String accessKeyId;

    @ConfigProperty(name = "secret_access_key")
    String secretAccessKey;

    private static final ErrorResponse INVALID_SIGNATURE_ERROR_RESPONSE = new ErrorResponse(
            ErrorResponseType.INVALID_SIGNATURE_EXCEPTION,
            "The request signature we calculated does not match the signature you provided. " +
                    "Check your AWS Secret Access Key and signing method. Consult the service documentation for details."
    );
    private static final RestResponse<ErrorResponse> RESPONSE_FORBIDDEN = RestResponse.status(
            Response.Status.FORBIDDEN,
            INVALID_SIGNATURE_ERROR_RESPONSE
    );

    @ServerRequestFilter
    public Optional<RestResponse<ErrorResponse>> getFilter(ContainerRequestContext ctx) {
        var authorizationHeader = ctx.getHeaderString("Authorization");
        if(null == authorizationHeader) {
            return Optional.of(RESPONSE_FORBIDDEN);
        }
        var region = getRegionFromAuthorization(authorizationHeader)
                .map(regionString -> Region.of(regionString));
        if(region.isEmpty()) {
            return Optional.of(RESPONSE_FORBIDDEN);
        }

        // The content stream needs to be duplicated, as we need to supply an additional one to the signature validator
        byte[] contentBytes;
        try {
            contentBytes = ctx.getEntityStream().readAllBytes();
        } catch (IOException e) {
            // Shouldn't happen
            throw new RuntimeException(e);
        }
        // Restore the consumed InputStream for the content to be processed down the chain (should auth be successful)
        ctx.setEntityStream(new ByteArrayInputStream(contentBytes));

        var aws4Signer = Aws4Signer.create();
        var sdkRequest = SdkHttpFullRequest.builder()
                .protocol("http")
                .host(ctx.getHeaderString("Host"))
                .method(SdkHttpMethod.POST)
                .appendHeader("Content-Type", ctx.getHeaderString("Content-Type"))
                .appendHeader("X-Amz-Date", ctx.getHeaderString("X-Amz-Date"))
                .appendHeader("X-Amz-Target", ctx.getHeaderString("X-Amz-Target"))
                .contentStreamProvider(() -> new ByteArrayInputStream(contentBytes))
                .build();
        var signerParams = Aws4SignerParams.builder()
                .awsCredentials(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                )
                .signingName("dynamodb")
                .signingRegion(region.get())
                .build();
        var signedRequest = aws4Signer.sign(sdkRequest, signerParams);
        var signedAuthorization = signedRequest.headers().get("Authorization").stream().findFirst().get();

        return Optional.of(authorizationHeader)
                .filter(authn -> ! authn.equals(signedAuthorization))
                .map(authn -> RESPONSE_FORBIDDEN);
    }

    private Optional<String> getRegionFromAuthorization(String authorization) {
        if(null == authorization) {
            return Optional.empty();
        }
        var authorizationItems = authorization.split(" ");
        if(authorizationItems.length < 2 || null == authorizationItems[1]) {
            return Optional.empty();
        }

        var pathItems = authorizationItems[1].split("/");
        if(pathItems.length < 3) {
            return Optional.empty();
        }

        return Optional.ofNullable(pathItems[2]);
    }
}
