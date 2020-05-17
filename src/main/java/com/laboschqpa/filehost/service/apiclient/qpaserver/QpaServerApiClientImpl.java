package com.laboschqpa.filehost.service.apiclient.qpaserver;

import com.laboschqpa.filehost.api.dto.IsUserAuthorizedToResourceResponseDto;
import com.laboschqpa.filehost.config.annotation.ExceptionWrappedApiClient;
import com.laboschqpa.filehost.exceptions.ApiClientException;
import com.laboschqpa.filehost.service.apiclient.AbstractApiClient;
import com.laboschqpa.filehost.service.apiclient.ApiCallerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;

@Service
@ExceptionWrappedApiClient
public class QpaServerApiClientImpl extends AbstractApiClient implements QpaServerApiClient {

    @Value("${apiClient.qpaServer.baseUrl}")
    private String qpaServerBaseUrl;

    @Value("${apiClient.qpaServer.sessionResolver.isUserAuthorizedToResource}")
    private String isAuthorizedToResourceUri;

    public QpaServerApiClientImpl(ApiCallerFactory apiCallerFactory) {
        super(apiCallerFactory);
    }

    @Override
    public IsUserAuthorizedToResourceResponseDto getIsAuthorizedToResource(String sessionCookieValue, GetIsUserAuthorizedToResourceDto getIsUserAuthorizedToResourceDto) {
        LinkedMultiValueMap<String, String> cookies = new LinkedMultiValueMap<>();
        cookies.add("SESSION", sessionCookieValue);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("AuthInterService", System.getProperty("auth.interservice.key"));
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, "application/json");
        try {
            return getRemoteAccountApiCaller().doCallAndThrowExceptionIfStatuscodeIsNot2xx(
                    IsUserAuthorizedToResourceResponseDto.class,
                    isAuthorizedToResourceUri,
                    HttpMethod.GET,
                    null,
                    BodyInserters.fromValue(getIsUserAuthorizedToResourceDto),
                    httpHeaders,
                    cookies,
                    false
            );
        } catch (ApiClientException e) {
            if (e.getHttpStatus() != null && e.getHttpStatus().is3xxRedirection())
                return IsUserAuthorizedToResourceResponseDto.builder().authenticated(false).authorized(false).build();//The user isn't logged in so got redirected.
            else
                throw e;
        }
    }


    @Override
    protected String getApiBaseUrl() {
        return qpaServerBaseUrl;
    }
}