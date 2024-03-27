package org.ht.swagger_demo.controller;

import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "用户信息注册")
public class Swagger {
    @RequestMapping
    public String hello(){
        return "hello";
    }
}
