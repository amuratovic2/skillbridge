package com.skillbridge.communication.controller;

import com.skillbridge.communication.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/diagnostics")
public class DiagnosticController {

    private final String applicationName;
    private final String port;
    private final String instanceId;

    public DiagnosticController(
        @Value("${spring.application.name:communication-service}") String applicationName,
        @Value("${server.port:0}") String port,
        @Value("${eureka.instance.instance-id:${spring.application.name:communication-service}:${server.port:0}}") String instanceId
    ) {
        this.applicationName = applicationName;
        this.port = port;
        this.instanceId = instanceId;
    }

    @GetMapping("/instance")
    public ApiResponse<?> instance() {
        return ApiResponse.ok(Map.of(
            "service", applicationName,
            "port", port,
            "instanceId", instanceId
        ));
    }
}
