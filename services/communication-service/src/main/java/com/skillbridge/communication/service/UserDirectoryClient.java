package com.skillbridge.communication.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.skillbridge.communication.dto.RemoteUserProfile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class UserDirectoryClient {

    private static final String USER_BY_ID_URL = "http://user-service/api/users/{id}";

    private final RestTemplate restTemplate;

    public UserDirectoryClient(RestTemplate loadBalancedRestTemplate) {
        this.restTemplate = loadBalancedRestTemplate;
    }

    public Optional<RemoteUserProfile> findActiveUser(Integer userId) {
        try {
            JsonNode response = restTemplate.getForObject(USER_BY_ID_URL, JsonNode.class, userId);
            if (response == null || !response.path("success").asBoolean(false)) {
                return Optional.empty();
            }

            JsonNode data = response.path("data");
            if (data.isMissingNode() || !data.path("isActive").asBoolean(false)) {
                return Optional.empty();
            }

            return Optional.of(new RemoteUserProfile(
                data.path("id").isNumber() ? data.path("id").asInt() : userId,
                data.path("username").asText(null),
                data.path("firstName").asText(null),
                data.path("lastName").asText(null),
                data.path("isActive").asBoolean()
            ));
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                return Optional.empty();
            }
            throw unavailable(ex);
        } catch (ResourceAccessException ex) {
            throw unavailable(ex);
        } catch (RestClientException ex) {
            throw unavailable(ex);
        }
    }

    private ResponseStatusException unavailable(Exception ex) {
        return new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE,
            "User service is unavailable; user data could not be validated",
            ex
        );
    }
}
