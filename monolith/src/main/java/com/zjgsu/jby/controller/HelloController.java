package com.zjgsu.jby.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HelloController {

    @GetMapping("/api/hello")
    public Map<String, String> hello() {
        return Map.of(
            "project", "商小淘 ShangXiaoTao",
            "description", "校园二手交易平台",
            "status", "running"
        );
    }
}
