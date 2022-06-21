/*
 * Copyright (C) 2022 Baidu, Inc. All Rights Reserved.
 */

/**
 * @class HomeController
 * @author xieyaowei(xieyaowei@baidu.com)
 * @date 2022-06-21 11:59
 * @brief
 *
 **/

package com.xiapistudio.http.webapp.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @RequestMapping("/")
    String home() {
        return "Hello World!";
    }

}