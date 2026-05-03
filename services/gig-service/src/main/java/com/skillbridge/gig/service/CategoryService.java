package com.skillbridge.gig.service;

import com.skillbridge.gig.dto.CategoryResponse;
import com.skillbridge.gig.mapper.GigMapper;
import com.skillbridge.gig.model.Category;
import com.skillbridge.gig.repository.CategoryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll(Sort.by("title").ascending()).stream()
            .map(GigMapper::toResponse)
            .toList();
    }

    public CategoryResponse findBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        return GigMapper.toResponse(category);
    }

    @Transactional
    public CategoryResponse create(String title) {
        String slug = title.toLowerCase().replaceAll("\\s+", "-");
        if (categoryRepository.findBySlug(slug).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists");
        }
        return GigMapper.toResponse(categoryRepository.save(new Category(title, slug)));
    }

    @Transactional
    public CategoryResponse update(Integer id, String title) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        category.setTitle(title);
        category.setSlug(title.toLowerCase().replaceAll("\\s+", "-"));
        return GigMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    public Map<String, String> delete(Integer id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        categoryRepository.delete(category);
        return Map.of("message", "Category deleted successfully");
    }
}
