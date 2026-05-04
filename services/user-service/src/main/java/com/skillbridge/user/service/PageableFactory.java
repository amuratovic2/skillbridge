package com.skillbridge.user.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

@Component
public class PageableFactory {

    private static final Map<String, String> USER_SORT_FIELDS = Map.of(
        "createdAt", "createdAt",
        "updatedAt", "updatedAt",
        "username", "username",
        "email", "email",
        "country", "country",
        "role", "role"
    );

    private static final Set<String> DESC_VALUES = Set.of("desc", "descending");
    private static final Set<String> ASC_VALUES = Set.of("asc", "ascending");

    public Pageable users(int page, int limit, String sortBy, String sortDirection) {
        String property = USER_SORT_FIELDS.get(sortBy);
        if (property == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort field: " + sortBy);
        }

        Sort.Direction direction = parseDirection(sortDirection);
        return PageRequest.of(page - 1, limit, Sort.by(direction, property));
    }

    private Sort.Direction parseDirection(String sortDirection) {
        String normalized = sortDirection == null ? "desc" : sortDirection.trim().toLowerCase();
        if (DESC_VALUES.contains(normalized)) {
            return Sort.Direction.DESC;
        }
        if (ASC_VALUES.contains(normalized)) {
            return Sort.Direction.ASC;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort direction: " + sortDirection);
    }
}
