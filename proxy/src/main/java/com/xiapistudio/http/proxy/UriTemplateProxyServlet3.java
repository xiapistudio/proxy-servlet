/*
 * Copyright (C) 2022 Baidu, Inc. All Rights Reserved.
 */

/**
 * @class UriTemplateProxyServlet3
 * @author xieyaowei(xieyaowei@baidu.com)
 * @date 2022-06-21 20:33
 * @brief
 *
 **/

package com.xiapistudio.http.proxy;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpHeaders;

public class UriTemplateProxyServlet3 extends UriTemplateProxyServlet2 {

    protected static final String COOKIE_TARGET_URI = "targetUri";

    @Override
    protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
            throws ServletException, IOException {
        String uri = servletRequest.getRequestURI();
        String refererUri = extractRefererUri(servletRequest.getHeader(HttpHeaders.REFERER));

        // TODO: 对于错误请求直接返回
        if (uri.toLowerCase().equals("/error")) {
            return;
        }

        // 获取上报的Cookies
        Map<String, Cookie> exitsCookies = new HashMap<>();
        Cookie[] cookies = servletRequest.getCookies();
        if (null != cookies) {
            for (Cookie cookie : cookies) {
                exitsCookies.put(cookie.getName(), cookie);
            }
        }

        // 存储 TargetUri 的 Cookie
        String name = getCookieNamePrefix(COOKIE_TARGET_URI) + COOKIE_TARGET_URI;

        // TODO: 不是以 / 结尾的，补齐 uri，不然无法 match
        uri = uri.endsWith("/") ? uri : uri + "/";
        refererUri = refererUri.endsWith("/") ? refererUri : refererUri + "/";

        if (pathMatcher.match(targetUriMapping, uri)) {
            initTargetUri(uri, servletRequest);

            // TODO: 增加 Cookie 信息 第一步（开始）
            Cookie cookie = new Cookie(name, targetUri);
            cookie.setPath("/");
            servletResponse.addCookie(cookie);
            // TODO: 增加 Cookie 信息 第一步（结束）

            super.superService(servletRequest, servletResponse);
        } else if (pathMatcher.match(targetUriMapping, refererUri)) {
            initTargetUri(refererUri, servletRequest);

            super.superService(servletRequest, servletResponse);
        } else if (exitsCookies.containsKey(name)) {
            // TODO: 增加 Cookie 信息 第二步（开始）
            Cookie cookie = exitsCookies.get(name);
            initTargetUri(cookie.getValue(), servletRequest);

            super.superService(servletRequest, servletResponse);
            // TODO: 增加 Cookie 信息 第二步（结束）
        } else {
            // 凡是不符合要求的则 404 返回
            servletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected String extractTargetInfo(String source, String template)
            throws ServletException {
        if (pathMatcher.match(targetUriMapping, source)) {
            return super.extractTargetInfo(source, template);
        } else {
            return source;
        }
    }
}