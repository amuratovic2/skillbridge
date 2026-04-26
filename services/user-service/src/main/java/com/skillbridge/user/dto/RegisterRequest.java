package com.skillbridge.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    @NotBlank @Size(max = 255) private String username;
    @NotBlank @Email @Size(max = 255) private String email;
    @NotBlank @Size(min = 6, max = 100) private String password;
    @NotBlank @Pattern(regexp = "CLIENT|FREELANCER|ADMIN", message = "must be CLIENT, FREELANCER or ADMIN") private String role;
    @Size(max = 255) private String firstName;
    @Size(max = 255) private String lastName;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
}
