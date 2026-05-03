package com.skillbridge.gig.service;

import com.skillbridge.gig.dto.PopularTagResponse;
import com.skillbridge.gig.dto.TagResponse;
import com.skillbridge.gig.mapper.GigMapper;
import com.skillbridge.gig.model.Tag;
import com.skillbridge.gig.repository.TagRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public List<TagResponse> findAll() {
        return tagRepository.findAllByOrderByNameAsc().stream()
            .map(GigMapper::toResponse)
            .toList();
    }

    public List<PopularTagResponse> findPopular(int limit) {
        return tagRepository.findPopularRaw(limit).stream()
            .map(row -> new PopularTagResponse(
                ((Number) row[0]).intValue(),
                (String) row[1],
                (String) row[2],
                ((Number) row[3]).longValue()
            ))
            .toList();
    }
}
