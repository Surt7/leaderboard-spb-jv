package com.worldpinball.leaderboard.api.controllers.Public;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/auth")
    public ResponseEntity<String> auth() {
        return ResponseEntity.ok("auth ok");
    }

    @GetMapping("/admin")
    public ResponseEntity<String> admin() {
        return ResponseEntity.ok("admin ok");
    }

    @GetMapping("/whoami")
    public ResponseEntity<?> whoAmI(
            @RequestHeader(value = "X-User-Sub", defaultValue = "dev-admin-sub") String sub,
            @RequestHeader(value = "X-User-Email", defaultValue = "admin@worldpinball.com") String email) {
        return ResponseEntity.ok(Map.of(
                "sub", sub,
                "email", email,
                "roleClaims", List.of("Admin")
        ));
    }
}
