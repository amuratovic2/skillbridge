package com.skillbridge.user.service;

import com.skillbridge.user.dto.PageResponse;
import com.skillbridge.user.dto.UpdateUserRequest;
import com.skillbridge.user.dto.UserResponse;
import com.skillbridge.user.mapper.UserMapper;
import com.skillbridge.user.model.User;
import com.skillbridge.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
        Page<User> userPage = userRepository.findByIsActiveTrue(
            PageRequest.of(page - 1, limit, Sort.by("createdAt").descending())
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
    public Map<String, String> deactivate(Integer id) {
        User user = findEntityById(id);
        user.setIsActive(false);
        userRepository.save(user);
        return Map.of("message", "User deactivated successfully");
    }
}
