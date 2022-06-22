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
import com.xiapistudio.http.proxy.UriTemplateProxyServlet4;

@Configuration
public class WebConfig {

    // @Bean
    // 通过 refer 和 location 实现代理转发
    public ServletRegistrationBean<?> servletRegistrationBean2() {
        String urlMapping = "/*";
        String targetUri = "{protocol}://{hostname}";
        String targetUriMapping = "/proxy/{protocol}/{hostname}/**";

        ServletRegistrationBean<?> servletRegistrationBean =
                new ServletRegistrationBean<>(new UriTemplateProxyServlet2(), urlMapping);
        servletRegistrationBean.addInitParameter("targetUri", targetUri);
        servletRegistrationBean.addInitParameter("targetUriMapping", targetUriMapping);
        servletRegistrationBean.addInitParameter(ProxyServlet.P_LOG, "true");
        servletRegistrationBean.addInitParameter(ProxyServlet.P_HANDLEREDIRECTS, "true");
        return servletRegistrationBean;
    }

    // @Bean
    // 通过 refer 和 cookie 实现代理转发
    public ServletRegistrationBean<?> servletRegistrationBean3() {
        String urlMapping = "/*";
        String targetUri = "{protocol}://{hostname}";
        String targetUriMapping = "/proxy/{protocol}/{hostname}/**";

        ServletRegistrationBean<?> servletRegistrationBean =
                new ServletRegistrationBean<>(new UriTemplateProxyServlet3(), urlMapping);
        servletRegistrationBean.addInitParameter("targetUri", targetUri);
        servletRegistrationBean.addInitParameter("targetUriMapping", targetUriMapping);
        servletRegistrationBean.addInitParameter(ProxyServlet.P_LOG, "true");
        servletRegistrationBean.addInitParameter(ProxyServlet.P_HANDLEREDIRECTS, "true");
        return servletRegistrationBean;
    }

    // http://localhost:8098/?protocol=http&hostname=www.baidu.com
    // 通过 refer 和 query 实现代理转发
    @Bean
    public ServletRegistrationBean<?> servletRegistrationBean4() {
        String urlMapping = "/*";
        String targetUri = "{protocol}://{hostname}";
        String targetUriMapping = "**protocol={protocol}&hostname={hostname}&**";

        ServletRegistrationBean<?> servletRegistrationBean =
                new ServletRegistrationBean<>(new UriTemplateProxyServlet4(), urlMapping);
        servletRegistrationBean.addInitParameter("targetUri", targetUri);
        servletRegistrationBean.addInitParameter("targetUriMapping", targetUriMapping);
        servletRegistrationBean.addInitParameter(ProxyServlet.P_LOG, "true");
        servletRegistrationBean.addInitParameter(ProxyServlet.P_HANDLEREDIRECTS, "true");
        return servletRegistrationBean;
    }

}