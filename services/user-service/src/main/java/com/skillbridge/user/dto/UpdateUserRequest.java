package com.skillbridge.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
    @Size(max = 255) String firstName,
    @Size(max = 255) String lastName,
    @Size(max = 2000) String bio,
    @Size(max = 500) String profilePicture,
    @Size(max = 100) String country
) {
}
