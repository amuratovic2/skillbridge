package com.skillbridge.user.service;

import com.skillbridge.user.model.User;
import com.skillbridge.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User findById(Integer id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public Map<String, Object> findAll(int page, int limit) {
        Page<User> userPage = userRepository.findByIsActiveTrue(
            PageRequest.of(page - 1, limit, Sort.by("createdAt").descending())
        );
        Map<String, Object> result = new HashMap<>();
        result.put("data", userPage.getContent());
        result.put("meta", Map.of(
            "total", userPage.getTotalElements(),
            "page", page,
            "limit", limit,
            "totalPages", userPage.getTotalPages()
        ));
        return result;
    }

    public User getPublicProfile(Integer id) {
        return userRepository.findByIdAndIsActiveTrue(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public User update(Integer id, Map<String, String> data) {
        User user = findById(id);
        if (data.containsKey("firstName")) user.setFirstName(data.get("firstName"));
        if (data.containsKey("lastName")) user.setLastName(data.get("lastName"));
        if (data.containsKey("bio")) user.setBio(data.get("bio"));
        if (data.containsKey("profilePicture")) user.setProfilePicture(data.get("profilePicture"));
        if (data.containsKey("country")) user.setCountry(data.get("country"));
        return userRepository.save(user);
    }

    public Map<String, String> deactivate(Integer id) {
        User user = findById(id);
        user.setIsActive(false);
        userRepository.save(user);
        return Map.of("message", "User deactivated successfully");
    }
}
