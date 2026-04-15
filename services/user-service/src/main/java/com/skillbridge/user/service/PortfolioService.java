package com.skillbridge.user.service;

import com.skillbridge.user.model.PortfolioItem;
import com.skillbridge.user.model.User;
import com.skillbridge.user.repository.PortfolioItemRepository;
import com.skillbridge.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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

    public List<PortfolioItem> findByUserId(Integer userId) {
        return portfolioItemRepository.findByUser_IdOrderByCreatedAtDesc(userId);
    }

    public PortfolioItem create(Integer userId, Map<String, String> data) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        PortfolioItem item = new PortfolioItem();
        item.setUser(user);
        item.setTitle(data.get("title"));
        item.setDescription(data.get("description"));
        item.setImageUrl(data.get("imageUrl"));
        return portfolioItemRepository.save(item);
    }

    public PortfolioItem update(Integer id, Integer userId, Map<String, String> data) {
        PortfolioItem item = findAndVerify(id, userId);
        if (data.containsKey("title")) item.setTitle(data.get("title"));
        if (data.containsKey("description")) item.setDescription(data.get("description"));
        if (data.containsKey("imageUrl")) item.setImageUrl(data.get("imageUrl"));
        return portfolioItemRepository.save(item);
    }

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
