package com.example.jejugilmoa.global.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/health")
    public String health() {
        return "ok";
    }

    @GetMapping("/")
    public String root() { return "ok"; }
}
