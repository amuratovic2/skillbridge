package com.skillbridge.user.controller;

import com.skillbridge.user.dto.ApiResponse;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

@RestController
@RequestMapping("/diagnostics")
public class DiagnosticController {

    private final String applicationName;
    private final String serverPort;
    private final String instanceId;

    public DiagnosticController(Environment environment) {
        this.applicationName = environment.getProperty("spring.application.name", "user-service");
        this.serverPort = environment.getProperty("server.port", "0");
        this.instanceId = environment.getProperty("eureka.instance.instance-id", applicationName + ":" + serverPort);
    }

    @GetMapping("/instance")
    public ApiResponse<?> instance() {
        return ApiResponse.ok(Map.of(
            "service", applicationName,
            "port", serverPort,
            "instanceId", instanceId,
            "host", hostname()
        ));
    }

    private String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            return "unknown";
        }
    }
}
