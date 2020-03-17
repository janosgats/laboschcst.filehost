package com.laboschcst.filehost.config.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.laboschcst.filehost.api.dto.InternalResourceDto;
import com.laboschcst.filehost.api.dto.IsUserAuthorizedToResourceResponseDto;
import com.laboschcst.filehost.api.dto.StoredFileDto;
import com.laboschcst.filehost.exceptions.UnAuthorizedException;
import com.laboschcst.filehost.service.apiclient.CsillagturaServerApiClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(1)
@RequiredArgsConstructor
public class AuthFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);

    private boolean authNSkipAll;

    private final CsillagturaServerApiClient csillagturaServerApiClient;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;

        if (authNSkipAll) {
            logger.trace("Skipping auth filter according to application.properties!");
            chain.doFilter(request, response);
        } else {
            try {
                StoredFileDto storedFileDto = null;

                String authInterServiceHeader = httpServletRequest.getHeader("AuthInterService");
                if (authInterServiceHeader != null && !authInterServiceHeader.isBlank()) {
                    //Authorizing call between services
                    if (!isAuthInterServiceHeaderValid(authInterServiceHeader))
                        throw new UnAuthorizedException("AuthInterService header is invalid.");
                } else {
                    //Authorizing call from a user
                    Cookie sessionCookie = getSessionCookie(httpServletRequest);
                    Long requestedStoredFileId = getRequestedStoredFileId(httpServletRequest);

                    InternalResourceDto requestedInternalResourceDto = InternalResourceDto.builder().storedFileId(requestedStoredFileId).build();
                    IsUserAuthorizedToResourceResponseDto responseDto = csillagturaServerApiClient.getIsAuthorizedToResource(sessionCookie.getValue(), requestedInternalResourceDto);

                    if (!responseDto.isAuthorized())
                        throw new UnAuthorizedException("User of the sent Session is unauthorized for the requested resource: " + requestedStoredFileId);

                    storedFileDto = responseDto.getStoredFileDto();
                }

                logger.trace("AuthFilter auth is valid.");
                chain.doFilter(new StoredFileServletRequestWrapper(httpServletRequest, storedFileDto), response);
            } catch (UnAuthorizedException e) {
                logger.trace("Request is unauthorized in AuthFilter: " + e.getMessage());
                writeErrorResponseBody((HttpServletResponse) response, "Unauthorized: " + e.getMessage(), HttpStatus.FORBIDDEN);
            } catch (Exception e) {
                logger.debug("Exception thrown while trying to authenticate incoming request!", e);
                writeErrorResponseBody((HttpServletResponse) response, "Exception thrown while trying to authenticate incoming request!", HttpStatus.INTERNAL_SERVER_ERROR);
            }

        }
    }

    private Long getRequestedStoredFileId(HttpServletRequest httpServletRequest) {
        String[] resourceIdParams = httpServletRequest.getParameterValues("storedFileId");
        if (resourceIdParams == null)
            throw new UnAuthorizedException("No storedFileId parameter is present.");

        if (resourceIdParams.length > 1)
            throw new UnAuthorizedException("Multi value is not allowed for storedFileId parameter.");
        else if (resourceIdParams.length == 0)
            throw new UnAuthorizedException("No storedFileId parameter is present.");

        return Long.parseLong(resourceIdParams[0]);
    }

    private Cookie getSessionCookie(HttpServletRequest httpServletRequest) {
        Cookie[] cookies = httpServletRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("SESSION".equals(cookie.getName())) {
                    return cookie;
                }
            }
        }
        throw new UnAuthorizedException("Cannot found session cookie!");
    }

    private boolean isAuthInterServiceHeaderValid(String authHeader) {
        return authHeader.equals(System.getProperty("auth.interservice.key"));
    }

    private void writeErrorResponseBody(HttpServletResponse httpServletResponse, String errorMessage, HttpStatus httpStatus) throws IOException {
        ObjectNode responseObjectNode = new ObjectNode(JsonNodeFactory.instance);
        responseObjectNode.put("error", errorMessage);
        String responseBody = responseObjectNode.toString();

        httpServletResponse.setContentType("application/json");
        httpServletResponse.setContentLength(responseBody.length());
        httpServletResponse.getWriter().write(responseBody);
        httpServletResponse.setStatus(httpStatus.value());
    }

    @Value("${authn.skip.all:false}")
    public void setAuthNSkipAll(Boolean authNSkipAll) {
        this.authNSkipAll = authNSkipAll;
    }
}