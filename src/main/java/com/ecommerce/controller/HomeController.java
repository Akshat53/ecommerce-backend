package com.ecommerce.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, String> home() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "E-commerce API is running");
        return response;
    }
    
    @GetMapping("/api/test")
    public Map<String, String> test() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Test endpoint is working");
        return response;
    }
}