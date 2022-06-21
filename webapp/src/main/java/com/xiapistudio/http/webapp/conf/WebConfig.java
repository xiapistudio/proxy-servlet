/*
 * Copyright (C) 2022 Baidu, Inc. All Rights Reserved.
 */

/**
 * @class WebConfig
 * @author xieyaowei(xieyaowei@baidu.com)
 * @date 2022-06-21 11:48
 * @brief
 *
 **/

package com.xiapistudio.http.webapp.conf;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.xiapistudio.http.proxy.ProxyServlet;
import com.xiapistudio.http.proxy.UriTemplateProxyServlet2;
import com.xiapistudio.http.proxy.UriTemplateProxyServlet3;

@Configuration
public class WebConfig {

    @Bean
    public ServletRegistrationBean<?> servletRegistrationBean() {
        String urlMapping = "/*";
        String targetUri = "{protocol}://{hostname}";
        String targetUriMapping = "/proxy/{protocol}/{hostname}/**";

        // 通过 refer 和 location 实现代理转发
        // ServletRegistrationBean<?> servletRegistrationBean =
        //        new ServletRegistrationBean<>(new UriTemplateProxyServlet2(), urlMapping);

        // 通过 refer 和 cookie 实现代理转发
        ServletRegistrationBean<?> servletRegistrationBean =
                new ServletRegistrationBean<>(new UriTemplateProxyServlet3(), urlMapping);
        servletRegistrationBean.addInitParameter("targetUri", targetUri);
        servletRegistrationBean.addInitParameter("targetUriMapping", targetUriMapping);
        servletRegistrationBean.addInitParameter(ProxyServlet.P_LOG, "true");
        servletRegistrationBean.addInitParameter(ProxyServlet.P_HANDLEREDIRECTS, "true");
        return servletRegistrationBean;
    }

}