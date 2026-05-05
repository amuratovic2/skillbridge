package com.skillbridge.communication.dto;

public record RemoteUserProfile(
    Integer id,
    String username,
    String firstName,
    String lastName,
    Boolean isActive
) {
}
