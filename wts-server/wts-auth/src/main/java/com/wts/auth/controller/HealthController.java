package com.wts.auth.controller;

import com.wts.common.result.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 健康检查与系统信息
 */
@RestController
public class HealthController {

    @GetMapping("/api/v1/health")
    public R<Map<String, Object>> health() {
        return R.ok(Map.of(
                "status", "UP",
                "service", "WTS Server",
                "version", "3.0.0",
                "time", LocalDateTime.now().toString()
        ));
    }
}
