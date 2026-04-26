package com.skillbridge.user.service;

import com.skillbridge.user.dto.CreatePortfolioItemRequest;
import com.skillbridge.user.dto.PortfolioItemResponse;
import com.skillbridge.user.dto.UpdatePortfolioItemRequest;
import com.skillbridge.user.mapper.UserMapper;
import com.skillbridge.user.model.PortfolioItem;
import com.skillbridge.user.model.User;
import com.skillbridge.user.repository.PortfolioItemRepository;
import com.skillbridge.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class PortfolioService {

    private final PortfolioItemRepository portfolioItemRepository;
    private final UserRepository userRepository;

    public PortfolioService(PortfolioItemRepository portfolioItemRepository, UserRepository userRepository) {
        this.portfolioItemRepository = portfolioItemRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<PortfolioItemResponse> findByUserId(Integer userId) {
        return portfolioItemRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
            .map(UserMapper::toResponse)
            .toList();
    }

    @Transactional
    public PortfolioItemResponse create(Integer userId, CreatePortfolioItemRequest data) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        PortfolioItem item = new PortfolioItem();
        item.setUser(user);
        item.setTitle(data.title().trim());
        item.setDescription(data.description());
        item.setImageUrl(data.imageUrl());
        return UserMapper.toResponse(portfolioItemRepository.save(item));
    }

    @Transactional
    public PortfolioItemResponse update(Integer id, Integer userId, UpdatePortfolioItemRequest data) {
        PortfolioItem item = findAndVerify(id, userId);
        if (data.title() != null) item.setTitle(data.title().trim());
        if (data.description() != null) item.setDescription(data.description());
        if (data.imageUrl() != null) item.setImageUrl(data.imageUrl());
        return UserMapper.toResponse(portfolioItemRepository.save(item));
    }

    @Transactional
    public Map<String, String> delete(Integer id, Integer userId) {
        PortfolioItem item = findAndVerify(id, userId);
        portfolioItemRepository.delete(item);
        return Map.of("message", "Portfolio item deleted successfully");
    }

    private PortfolioItem findAndVerify(Integer id, Integer userId) {
        PortfolioItem item = portfolioItemRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio item not found"));
        if (!item.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only modify your own portfolio items");
        }
        return item;
    }
}
