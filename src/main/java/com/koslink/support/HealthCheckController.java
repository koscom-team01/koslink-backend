package com.koslink.support;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 헬스체크 컨트롤러
 * 쿠버네티스 Liveness Probe 및 Readiness Probe 지원 (GET / 및 GET /health)
 */
@RestController
public class HealthCheckController {

    @GetMapping({"/", "/health"})
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK");
    }
}
