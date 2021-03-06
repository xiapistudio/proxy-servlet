/*
 * Copyright (C) 2022 Baidu, Inc. All Rights Reserved.
 */

/**
 * @class UriTemplateProxyServlet2
 * @author xieyaowei(xieyaowei@baidu.com)
 * @date 2022-06-21 10:53
 * @brief
 *
 **/

package com.xiapistudio.http.proxy;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.utils.URIUtils;

import com.xiapistudio.http.proxy.util.AntPathMatcher;
import com.xiapistudio.http.proxy.util.StringUtils;

public class UriTemplateProxyServlet2 extends ProxyServlet {

    protected static final String P_TARGET_URI_MAPPING = "targetUriMapping";
    protected static final String ATTR_IS_REDIRECT = UriTemplateProxyServlet2.class.getSimpleName() + ".isRedirect";
    protected static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{(.+?)\\}");

    protected String targetUriTemplate; // e.g.: {protocol}://{hostname}
    protected String targetUriMapping;  // e.g.: /proxy/{protocol}/{hostname}/**

    protected AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void initTarget() throws ServletException {
        targetUriTemplate = getConfigParam(P_TARGET_URI);
        if (!StringUtils.hasLength(targetUriTemplate)) {
            throw new ServletException(P_TARGET_URI + " is required.");
        }

        targetUriMapping = getConfigParam(P_TARGET_URI_MAPPING);
        if (!StringUtils.hasLength(targetUriMapping)) {
            throw new ServletException(P_TARGET_URI_MAPPING + " is required.");
        }
    }

    @Override
    protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
            throws ServletException, IOException {

        String uri = servletRequest.getRequestURI();
        String host = servletRequest.getHeader(HttpHeaders.HOST);
        String refererUri = extractRefererUri(servletRequest.getHeader(HttpHeaders.REFERER));

        // TODO: ??????????????????????????????
        if (uri.toLowerCase().equals("/error")) {
            return;
        }

        // TODO: ????????? / ?????????????????? uri??????????????? match
        uri = uri.endsWith("/") ? uri : uri + "/";
        refererUri = refererUri.endsWith("/") ? refererUri : refererUri + "/";

        if (pathMatcher.match(targetUriMapping, uri)) {
            initTargetUri(uri, servletRequest);

            super.service(servletRequest, servletResponse);
        } else if (pathMatcher.match(targetUriMapping, refererUri)) {
            initTargetUri(refererUri, servletRequest);

            // TODO: ?????? Location ?????? ?????????????????????
            String path = rewriteUrlFromRequest(servletRequest).replace(targetUri, "");
            String match = pathMatcher.extractPathWithinPattern(targetUriMapping, refererUri);
            match = match.startsWith("/") ? match : "/" + match;
            String proxy = StringUtils.trimTrailingCharacter(refererUri, '/').replace(match, "");
            String location = String.format("%s://%s%s%s", servletRequest.getScheme(), host, proxy, path);

            servletRequest.setAttribute(ATTR_IS_REDIRECT, true);
            servletRequest.setAttribute(HttpHeaders.LOCATION, location);
            // TODO: ?????? Location ?????? ?????????????????????

            super.service(servletRequest, servletResponse);
        } else {
            // ??????????????????????????? 404 ??????
            servletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    protected void superService(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
            throws ServletException, IOException {
        super.service(servletRequest, servletResponse);
    }

    @Override
    protected String rewritePathInfoFromRequest(HttpServletRequest servletRequest) {
        String path = super.rewritePathInfoFromRequest(servletRequest);

        // ???????????? "/" ??????????????? PATH Matcher
        path = path.endsWith("/") ? path : path + "/";

        if (pathMatcher.match(targetUriMapping, path)) {
            path = pathMatcher.extractPathWithinPattern(targetUriMapping, path);
        }

        path = path.startsWith("/") ? path : "/" + path;

        System.out.println("path:" + path);

        // ???????????? "/" ??????????????? PATH ?????????
        return StringUtils.trimTrailingCharacter(path, '/');
    }

    @Override
    protected void copyResponseHeaders(HttpResponse proxyResponse,
            HttpServletRequest servletRequest, HttpServletResponse servletResponse) {

        // TODO: ?????? Location ?????? ?????????????????????
        if (servletRequest.getAttribute(ATTR_IS_REDIRECT) != null &&
                (boolean) servletRequest.getAttribute(ATTR_IS_REDIRECT)) {
            if (proxyResponse.getEntity().getContentType().toString().toLowerCase().contains("text/html")) {
                servletResponse.setHeader(HttpHeaders.LOCATION,
                        (String) servletRequest.getAttribute(HttpHeaders.LOCATION));
                servletResponse.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            }
        }
        // TODO: ?????? Location ?????? ?????????????????????

        super.copyResponseHeaders(proxyResponse, servletRequest, servletResponse);
    }

    /**
     * ???????????????????????????
     * @param uri
     * @throws ServletException
     */
    protected void initTargetUri(String uri, HttpServletRequest servletRequest) throws ServletException {
        targetUri = extractTargetInfo(uri, targetUriTemplate);

        System.out.println("targetUri:" + targetUri);

        try {
            targetUriObj = new URI(targetUri);
        } catch (Exception e) {
            throw new ServletException("Rewritten targetUri is invalid: " + targetUri, e);
        }

        targetHost = URIUtils.extractHost(targetUriObj);

        if (servletRequest.getAttribute(ATTR_TARGET_URI) == null) {
            servletRequest.setAttribute(ATTR_TARGET_URI, targetUri);
        }

        if (servletRequest.getAttribute(ATTR_TARGET_HOST) == null) {
            servletRequest.setAttribute(ATTR_TARGET_HOST, targetHost);
        }
    }

    /**
     * ??????url??????????????????
     * @param source
     * @param template
     * @return
     * @throws ServletException
     */
    protected String extractTargetInfo(String source, String template)
            throws ServletException {
        Map<String, String> result = pathMatcher.extractUriTemplateVariables(targetUriMapping, source);

        StringBuffer urlBuf = new StringBuffer();
        Matcher matcher = TEMPLATE_PATTERN.matcher(template);
        while (matcher.find()) {
            String name = matcher.group(1);
            String value = result.remove(name); // note we remove
            if (value == null) {
                throw new ServletException("Missing HTTP info " + name + " to fill the template");
            }
            matcher.appendReplacement(urlBuf, value);
        }

        matcher.appendTail(urlBuf);

        return urlBuf.toString();
    }

    /**
     * ??? referer ????????? path
     * @param referer
     * @return
     * @throws ServletException
     */
    protected String extractRefererUri(String referer)
            throws ServletException {
        String preUri = "/";
        try {
            preUri = StringUtils.hasLength(referer) ? (new URI(referer)).getPath() : "/";
        } catch (Exception e) {
            throw new ServletException("Rewritten targetUri is invalid: " + referer, e);
        }

        return preUri;
    }

}