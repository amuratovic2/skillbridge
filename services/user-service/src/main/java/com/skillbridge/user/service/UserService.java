package com.skillbridge.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.skillbridge.user.dto.PageResponse;
import com.skillbridge.user.dto.UpdateUserRequest;
import com.skillbridge.user.dto.UserPatchState;
import com.skillbridge.user.dto.UserResponse;
import com.skillbridge.user.mapper.UserMapper;
import com.skillbridge.user.model.User;
import com.skillbridge.user.model.UserRole;
import com.skillbridge.user.repository.UserRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

@Service
public class UserService {

    private static final Set<String> PATCHABLE_FIELDS = Set.of(
        "firstName",
        "lastName",
        "bio",
        "profilePicture",
        "country"
    );

    private final UserRepository userRepository;
    private final PageableFactory pageableFactory;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public UserService(UserRepository userRepository, PageableFactory pageableFactory,
                       ObjectMapper objectMapper, Validator validator) {
        this.userRepository = userRepository;
        this.pageableFactory = pageableFactory;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public User findEntityById(Integer id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Integer id) {
        User user = findEntityById(id);
        return UserMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> findAll(int page, int limit) {
        return findAll(page, limit, "createdAt", "desc", null, null, null, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> findAll(int page, int limit, String sortBy, String sortDirection,
                                              String query, UserRole role, String country, String skill) {
        Page<User> userPage = userRepository.searchActiveUsers(
            blankToNull(query),
            role,
            blankToNull(country),
            blankToNull(skill),
            pageableFactory.users(page, limit, sortBy, sortDirection)
        );
        return new PageResponse<>(
            userPage.getContent().stream().map(UserMapper::toSummary).toList(),
            Map.of(
            "total", userPage.getTotalElements(),
            "page", page,
            "limit", limit,
            "totalPages", userPage.getTotalPages()
            )
        );
    }

    @Transactional(readOnly = true)
    public UserResponse getPublicProfile(Integer id) {
        User user = userRepository.findByIdAndIsActiveTrue(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return UserMapper.toResponse(user);
    }

    @Transactional
    public UserResponse update(Integer id, UpdateUserRequest data) {
        User user = findEntityById(id);
        if (data.firstName() != null) user.setFirstName(data.firstName());
        if (data.lastName() != null) user.setLastName(data.lastName());
        if (data.bio() != null) user.setBio(data.bio());
        if (data.profilePicture() != null) user.setProfilePicture(data.profilePicture());
        if (data.country() != null) user.setCountry(data.country());
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse patch(Integer id, JsonPatch patch) {
        User user = findEntityById(id);
        try {
            JsonNode current = objectMapper.convertValue(UserPatchState.from(user), JsonNode.class);
            JsonNode patched = patch.apply(current);
            validatePatchDocument(patched);

            UserPatchState state = objectMapper.treeToValue(patched, UserPatchState.class);
            var violations = validator.validate(state);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }

            state.applyTo(user);
            return UserMapper.toResponse(userRepository.save(user));
        } catch (JsonPatchException | JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON Patch document");
        }
    }

    @Transactional
    public Map<String, String> deactivate(Integer id) {
        User user = findEntityById(id);
        user.setIsActive(false);
        userRepository.save(user);
        return Map.of("message", "User deactivated successfully");
    }

    private void validatePatchDocument(JsonNode patched) {
        if (!patched.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Patched user profile must be an object");
        }
        patched.fieldNames().forEachRemaining(field -> {
            if (!PATCHABLE_FIELDS.contains(field)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Field cannot be patched: " + field);
            }
        });
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
