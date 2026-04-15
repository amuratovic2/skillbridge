package com.skillbridge.gig.service;

import com.skillbridge.gig.dto.CreateGigRequest;
import com.skillbridge.gig.dto.UpdateGigRequest;
import com.skillbridge.gig.model.Category;
import com.skillbridge.gig.model.Gig;
import com.skillbridge.gig.model.GigStatus;
import com.skillbridge.gig.model.Tag;
import com.skillbridge.gig.repository.CategoryRepository;
import com.skillbridge.gig.repository.GigRepository;
import com.skillbridge.gig.repository.TagRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GigService {

    private final GigRepository gigRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;

    public GigService(GigRepository gigRepository, CategoryRepository categoryRepository, TagRepository tagRepository) {
        this.gigRepository = gigRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
    }

    @Transactional
    public Gig create(Integer freelancerId, CreateGigRequest req) {
        Category category = categoryRepository.findById(req.getCategoryId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        Gig gig = new Gig();
        gig.setFreelancerId(freelancerId);
        gig.setCategory(category);
        gig.setTitle(req.getTitle());
        gig.setDescription(req.getDescription());
        gig.setCost(req.getCost());
        gig.setDeliveryTime(req.getDeliveryTime());
        gig.setRevisionCount(req.getRevisionCount());
        gig.setCoverImage(req.getCoverImage());

        gig = gigRepository.save(gig);

        if (req.getTags() != null && !req.getTags().isEmpty()) {
            syncTags(gig, req.getTags());
            gig = gigRepository.save(gig);
        }

        return findById(gig.getId());
    }

    @Transactional(readOnly = true)
    public Gig findById(Integer id) {
        Gig gig = gigRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gig not found"));
        // Force lazy load of tags and images within transaction
        gig.getTags().size();
        gig.getImages().size();
        return gig;
    }

    @Transactional
    public Gig update(Integer id, Integer freelancerId, UpdateGigRequest req) {
        Gig gig = gigRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gig not found"));

        if (!gig.getFreelancerId().equals(freelancerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only edit your own gigs");
        }

        if (req.getTitle() != null) gig.setTitle(req.getTitle());
        if (req.getDescription() != null) gig.setDescription(req.getDescription());
        if (req.getCost() != null) gig.setCost(req.getCost());
        if (req.getDeliveryTime() != null) gig.setDeliveryTime(req.getDeliveryTime());
        if (req.getRevisionCount() != null) gig.setRevisionCount(req.getRevisionCount());
        if (req.getCoverImage() != null) gig.setCoverImage(req.getCoverImage());
        if (req.getStatus() != null) gig.setStatus(GigStatus.valueOf(req.getStatus()));

        if (req.getCategoryId() != null) {
            Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
            gig.setCategory(category);
        }

        if (req.getTags() != null) {
            syncTags(gig, req.getTags());
        }

        gigRepository.save(gig);
        return findById(id);
    }

    @Transactional
    public Map<String, String> delete(Integer id, Integer freelancerId) {
        Gig gig = gigRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gig not found"));

        if (!gig.getFreelancerId().equals(freelancerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own gigs");
        }

        gig.setStatus(GigStatus.DELETED);
        gigRepository.save(gig);
        return Map.of("message", "Gig deleted successfully");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> search(String q, Integer categoryId, BigDecimal minPrice,
                                       BigDecimal maxPrice, Integer deliveryTime,
                                       String sortBy, int page, int limit) {
        limit = Math.min(limit, 100);

        Specification<Gig> spec = (root, query, cb) -> cb.equal(root.get("status"), GigStatus.ACTIVE);

        if (q != null && !q.isBlank()) {
            String pattern = "%" + q.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("description")), pattern)
            ));
        }

        if (categoryId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }

        if (minPrice != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("cost"), minPrice));
        }

        if (maxPrice != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("cost"), maxPrice));
        }

        if (deliveryTime != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("deliveryTime"), deliveryTime));
        }

        Sort sort = switch (sortBy != null ? sortBy : "") {
            case "price_asc" -> Sort.by("cost").ascending();
            case "price_desc" -> Sort.by("cost").descending();
            default -> Sort.by("createdAt").descending();
        };

        Page<Gig> result = gigRepository.findAll(spec, PageRequest.of(page - 1, limit, sort));

        // Force lazy load within transaction
        result.getContent().forEach(gig -> {
            gig.getTags().size();
            gig.getImages().size();
        });

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("total", result.getTotalElements());
        meta.put("page", page);
        meta.put("limit", limit);
        meta.put("totalPages", result.getTotalPages());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", result.getContent());
        response.put("meta", meta);
        return response;
    }

    @Transactional(readOnly = true)
    public List<Gig> findByFreelancerId(Integer freelancerId) {
        List<Gig> gigs = gigRepository.findByFreelancerIdAndStatusNotOrderByCreatedAtDesc(freelancerId, GigStatus.DELETED);
        gigs.forEach(gig -> gig.getTags().size());
        return gigs;
    }

    @Transactional(readOnly = true)
    public List<Gig> getFeatured(int limit) {
        Page<Gig> page = gigRepository.findAll(
            (root, query, cb) -> cb.equal(root.get("status"), GigStatus.ACTIVE),
            PageRequest.of(0, limit, Sort.by("createdAt").descending())
        );
        page.getContent().forEach(gig -> gig.getTags().size());
        return page.getContent();
    }

    private void syncTags(Gig gig, List<String> tagNames) {
        gig.getTags().clear();
        List<Tag> resolved = new ArrayList<>();
        for (String name : tagNames) {
            String slug = name.toLowerCase().replaceAll("\\s+", "-");
            Tag tag = tagRepository.findBySlug(slug)
                .orElseGet(() -> tagRepository.save(new Tag(name, slug)));
            resolved.add(tag);
        }
        gig.getTags().addAll(resolved);
    }
}
