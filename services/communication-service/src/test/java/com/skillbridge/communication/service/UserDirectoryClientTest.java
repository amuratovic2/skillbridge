package com.skillbridge.communication.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDirectoryClientTest {

    private static final String USER_URL = "http://user-service/api/users/{id}";
    private static final String USER_DIAGNOSTICS_URL = "http://user-service/api/diagnostics/instance";

    @Mock
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void findActiveUserReturnsProfileFromUserService() throws Exception {
        when(restTemplate.getForObject(USER_URL, com.fasterxml.jackson.databind.JsonNode.class, 2))
            .thenReturn(objectMapper.readTree("""
                {
                  "success": true,
                  "data": {
                    "id": 2,
                    "username": "lejla",
                    "firstName": "Lejla",
                    "lastName": "Hadzic",
                    "isActive": true
                  }
                }
                """));

        var result = new UserDirectoryClient(restTemplate).findActiveUser(2);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(2);
        assertThat(result.get().username()).isEqualTo("lejla");
        assertThat(result.get().isActive()).isTrue();
    }

    @Test
    void findActiveUserReturnsEmptyForInactiveUser() throws Exception {
        when(restTemplate.getForObject(USER_URL, com.fasterxml.jackson.databind.JsonNode.class, 2))
            .thenReturn(objectMapper.readTree("""
                {
                  "success": true,
                  "data": {
                    "id": 2,
                    "username": "disabled",
                    "isActive": false
                  }
                }
                """));

        var result = new UserDirectoryClient(restTemplate).findActiveUser(2);

        assertThat(result).isEmpty();
    }

    @Test
    void findActiveUserReturnsEmptyForNotFound() {
        when(restTemplate.getForObject(USER_URL, com.fasterxml.jackson.databind.JsonNode.class, 404))
            .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not found", null, null, null));

        var result = new UserDirectoryClient(restTemplate).findActiveUser(404);

        assertThat(result).isEmpty();
    }

    @Test
    void findActiveUserReturnsServiceUnavailableWhenUserServiceCannotBeReached() {
        when(restTemplate.getForObject(USER_URL, com.fasterxml.jackson.databind.JsonNode.class, 2))
            .thenThrow(new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> new UserDirectoryClient(restTemplate).findActiveUser(2))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
            .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void getUserServiceInstanceReturnsDiagnosticsFromLoadBalancedUserService() throws Exception {
        when(restTemplate.getForObject(USER_DIAGNOSTICS_URL, com.fasterxml.jackson.databind.JsonNode.class))
            .thenReturn(objectMapper.readTree("""
                {
                  "success": true,
                  "data": {
                    "service": "user-service",
                    "port": "3001",
                    "instanceId": "user-service:3001"
                  }
                }
                """));

        var result = new UserDirectoryClient(restTemplate).getUserServiceInstance();

        assertThat(result.path("data").path("instanceId").asText()).isEqualTo("user-service:3001");
    }
}
