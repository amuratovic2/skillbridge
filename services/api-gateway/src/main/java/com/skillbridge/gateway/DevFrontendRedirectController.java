package com.skillbridge.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class DevFrontendRedirectController {

    private final String frontendUrl;

    public DevFrontendRedirectController(
        @Value("${frontend.dev-url:http://localhost:4200}") String frontendUrl
    ) {
        this.frontendUrl = frontendUrl;
    }

    @GetMapping("/")
    public ResponseEntity<Void> redirectToFrontend() {
        return ResponseEntity
            .status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, URI.create(frontendUrl).toString())
            .build();
    }
}
