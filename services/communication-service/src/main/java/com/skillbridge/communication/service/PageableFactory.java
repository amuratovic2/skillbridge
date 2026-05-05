package com.skillbridge.communication.service;

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

    private static final Set<String> DIRECTIONS = Set.of("asc", "desc");

    public Pageable build(int page, int limit, String sortBy, String direction, Map<String, String> allowedSorts) {
        String normalizedDirection = direction == null ? "desc" : direction.toLowerCase();
        if (!DIRECTIONS.contains(normalizedDirection)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sort direction must be asc or desc");
        }

        String property = allowedSorts.get(sortBy);
        if (property == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort field: " + sortBy);
        }

        Sort.Direction sortDirection = Sort.Direction.fromString(normalizedDirection);
        return PageRequest.of(page - 1, limit, Sort.by(sortDirection, property));
    }
}
