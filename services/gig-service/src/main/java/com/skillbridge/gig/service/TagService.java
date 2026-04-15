package com.skillbridge.gig.service;

import com.skillbridge.gig.model.Tag;
import com.skillbridge.gig.repository.TagRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public List<Tag> findAll() {
        return tagRepository.findAllByOrderByNameAsc();
    }

    public List<Map<String, Object>> findPopular(int limit) {
        return tagRepository.findPopularRaw(limit).stream()
            .map(row -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", row[0]);
                map.put("name", row[1]);
                map.put("slug", row[2]);
                map.put("gigCount", ((Number) row[3]).longValue());
                return map;
            })
            .toList();
    }
}
