/*
 * Copyright (C) 2022 Baidu, Inc. All Rights Reserved.
 */

/**
 * @class UriTemplateProxyServlet4Tests
 * @author xieyaowei(xieyaowei@baidu.com)
 * @date 2022-06-22 14:47
 * @brief
 *
 **/

package com.xiapistudio.http.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.xiapistudio.http.proxy.util.AntPathMatcher;

public class UriTemplateProxyServlet4Tests {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Test
    void match() {
        assertThat(pathMatcher.match("protocol={protocol}&hostname={hostname}**",
                "protocol=http&hostname=www.baidu.com&other=other")).isTrue();
        assertThat(pathMatcher.match("protocol={protocol}&hostname={hostname}**",
                "?protocol=http&hostname=www.baidu.com&other=other")).isFalse();
        assertThat(pathMatcher.match("**protocol={protocol}&hostname={hostname}**",
                "?protocol=http&hostname=www.baidu.com&other=other")).isTrue();
        assertThat(pathMatcher.match("**protocol={protocol}&hostname={hostname}**",
                "127.0.0.1:8088?protocol=http&hostname=www.baidu.com")).isTrue();
        assertThat(pathMatcher.match("**protocol={protocol}&hostname={hostname}**",
                "127.0.0.1:8088?protocol=http&hostname=10.0.0.11:9941")).isTrue();
        assertThat(pathMatcher.match("**protocol={protocol}&hostname={hostname}&**",
                "127.0.0.1:8088?protocol=http&hostname=10.0.0.11:9941&")).isTrue();
    }

    @Test
    void extractUriTemplateVariables() throws Exception {
        Map<String, String> result = pathMatcher.extractUriTemplateVariables(
                "**protocol={protocol}&hostname={hostname}&**",
                "127.0.0.1:8088?protocol=http&hostname=10.0.0.11:9941&");

        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("protocol", "http");
        expected.put("hostname", "10.0.0.11:9941");
        assertThat(result).isEqualTo(expected);
    }

}