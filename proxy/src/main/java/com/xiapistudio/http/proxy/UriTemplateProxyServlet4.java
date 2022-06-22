/*
 * Copyright (C) 2022 Baidu, Inc. All Rights Reserved.
 */

/**
 * @class UriTemplateProxyServlet4
 * @author xieyaowei(xieyaowei@baidu.com)
 * @date 2022-06-22 14:29
 * @brief
 *
 **/

package com.xiapistudio.http.proxy;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;

import com.xiapistudio.http.proxy.util.AntPathMatcher;
import com.xiapistudio.http.proxy.util.StringUtils;

public class UriTemplateProxyServlet4 extends ProxyServlet {

    protected static final String P_TARGET_URI_MAPPING = "targetUriMapping";
    protected static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{(.+?)\\}");
    protected static final String ATTR_IS_REDIRECT = UriTemplateProxyServlet4.class.getSimpleName() + ".isRedirect";
    protected static final String ATTR_QUERY_STRING = UriTemplateProxyServlet4.class.getSimpleName() + ".queryString";

    protected String targetUriTemplate; // e.g.: {protocol}://{hostname}
    protected String targetUriMapping;  // e.g.: **protocol={protocol}&hostname={hostname}&**

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
        String host = servletRequest.getHeader(HttpHeaders.HOST);
        String query = StringUtils.hasLength(servletRequest.getQueryString()) ? servletRequest.getQueryString() : "&";
        String refererQuery = extractRefererQuery(servletRequest.getHeader(HttpHeaders.REFERER));

        // TODO: 不是以 & 结尾的，补齐 query，不然无法 match
        query = query.endsWith("&") ? query : query + "&";
        refererQuery = refererQuery.endsWith("&") ? refererQuery : refererQuery + "&";

        System.out.println("query:" + query + " refererQuery:" + refererQuery);


        if (!query.equals("&") && pathMatcher.match(targetUriMapping, query)) {
            LinkedHashMap<String, String> params = initTargetUri(query, servletRequest);
            String newQuery = createQuery(params);
            servletRequest.setAttribute(ATTR_QUERY_STRING, newQuery);

            super.service(servletRequest, servletResponse);
        } else if (!refererQuery.equals("&") && pathMatcher.match(targetUriMapping, refererQuery)) {
            initTargetUri(refererQuery, servletRequest);

            // TODO: 增加 Location 跳转 第一步（开始）
            String path = rewriteUrlFromRequest(servletRequest).replace(targetUri, "");
            Map<String, String> result = pathMatcher.extractUriTemplateVariables(targetUriTemplate, targetUri);
            LinkedHashMap<String, String> params = new LinkedHashMap<>();
            params.putAll(result);
            params.putAll(parseQuery(query));
            String newQuery = createQuery(params);
            String location = String.format("%s://%s%s?%s", servletRequest.getScheme(), host, path, newQuery);
            System.out.println("location:" + location);
            servletRequest.setAttribute(ATTR_IS_REDIRECT, true);
            servletRequest.setAttribute(HttpHeaders.LOCATION, location);
            // TODO: 增加 Location 跳转 第一步（结束）

            servletRequest.setAttribute(ATTR_QUERY_STRING, StringUtils.trimTrailingCharacter(query, '&'));

            super.service(servletRequest, servletResponse);
        }

    }

    @Override
    protected String rewriteQueryStringFromRequest(HttpServletRequest servletRequest, String queryString) {
        return (String) servletRequest.getAttribute(ATTR_QUERY_STRING);
    }

    @Override
    protected void copyResponseHeaders(HttpResponse proxyResponse,
            HttpServletRequest servletRequest, HttpServletResponse servletResponse) {

        // TODO: 增加 Location 跳转 第二步（开始）
        if (servletRequest.getAttribute(ATTR_IS_REDIRECT) != null &&
                (boolean) servletRequest.getAttribute(ATTR_IS_REDIRECT)) {
            if (proxyResponse.getEntity().getContentType().toString().toLowerCase().contains("text/html")) {
                servletResponse.setHeader(HttpHeaders.LOCATION,
                        (String) servletRequest.getAttribute(HttpHeaders.LOCATION));
                servletResponse.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            }
        }
        // TODO: 增加 Location 跳转 第二步（结束）

        super.copyResponseHeaders(proxyResponse, servletRequest, servletResponse);
    }


    protected LinkedHashMap<String, String> initTargetUri(String query, HttpServletRequest servletRequest)
            throws ServletException {
        LinkedHashMap<String, String> params = parseQuery(query);

        // Now rewrite the URL
        StringBuffer urlBuf = new StringBuffer(); // note: StringBuilder isn't supported by Matcher
        Matcher matcher = TEMPLATE_PATTERN.matcher(targetUriTemplate);
        while (matcher.find()) {
            String arg = matcher.group(1);
            String replacement = params.remove(arg); // note we remove
            if (replacement == null) {
                throw new ServletException("Missing HTTP parameter " + arg + " to fill the template");
            }
            matcher.appendReplacement(urlBuf, replacement);
        }

        matcher.appendTail(urlBuf);

        targetUri = urlBuf.toString();

        servletRequest.setAttribute(ATTR_TARGET_URI, targetUri);
        try {
            targetUriObj = new URI(targetUri);
        } catch (Exception e) {
            throw new ServletException("Rewritten targetUri is invalid: " + targetUri, e);
        }

        targetHost = URIUtils.extractHost(targetUriObj);

        servletRequest.setAttribute(ATTR_TARGET_HOST, targetHost);

        return params;
    }

    private LinkedHashMap<String, String> parseQuery(String query)
            throws ServletException {
        // TODO: 去除人为添加的 & 结尾
        query = StringUtils.trimTrailingCharacter(query, '&');

        String queryString = "";
        if (query != null) {
            queryString = "?" + query; // no "?" but might have "#"
        }

        int hash = queryString.indexOf('#');
        if (hash >= 0) {
            queryString = queryString.substring(0, hash);
        }

        List<NameValuePair> pairs;
        try {
            // note: HttpClient 4.2 lets you parse the string without building the URI
            pairs = URLEncodedUtils.parse(new URI(queryString), "UTF-8");
        } catch (URISyntaxException e) {
            throw new ServletException("Unexpected URI parsing error on " + queryString, e);
        }

        LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
        for (NameValuePair pair : pairs) {
            params.put(pair.getName(), pair.getValue());
        }

        return params;
    }

    private String createQuery(LinkedHashMap<String, String> params)
            throws IOException {

        // Determine the new query string based on removing the used names
        StringBuilder newQueryBuf = new StringBuilder();
        for (Map.Entry<String, String> nameVal : params.entrySet()) {
            if (newQueryBuf.length() > 0) {
                newQueryBuf.append('&');
            }

            newQueryBuf.append(nameVal.getKey()).append('=');
            if (nameVal.getValue() != null) {
                newQueryBuf.append(URLEncoder.encode(nameVal.getValue(), "UTF-8"));
            }
        }

      return newQueryBuf.toString();
    }

    /**
     * 从 referer 中提取 query
     * @param referer
     * @return
     * @throws ServletException
     */
    protected String extractRefererQuery(String referer)
            throws ServletException {
        String query = "&";
        try {
            query = StringUtils.hasLength(referer) ? (new URI(referer)).getQuery() : "&";
        } catch (Exception e) {
            throw new ServletException("Rewritten targetUri is invalid: " + referer, e);
        }

        return StringUtils.hasLength(query) ? query : "&";
    }

}