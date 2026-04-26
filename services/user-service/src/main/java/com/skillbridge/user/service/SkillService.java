package com.skillbridge.user.service;

import com.skillbridge.user.dto.CreateSkillRequest;
import com.skillbridge.user.dto.SkillResponse;
import com.skillbridge.user.mapper.UserMapper;
import com.skillbridge.user.model.Skill;
import com.skillbridge.user.model.User;
import com.skillbridge.user.repository.SkillRepository;
import com.skillbridge.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class SkillService {

    private final SkillRepository skillRepository;
    private final UserRepository userRepository;

    public SkillService(SkillRepository skillRepository, UserRepository userRepository) {
        this.skillRepository = skillRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> findAll() {
        return skillRepository.findAll().stream().map(UserMapper::toResponse).toList();
    }

    @Transactional
    public SkillResponse create(CreateSkillRequest request) {
        String name = request.name().trim();
        if (skillRepository.findByName(name).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Skill already exists");
        }
        Skill skill = new Skill();
        skill.setName(name);
        return UserMapper.toResponse(skillRepository.save(skill));
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> getUserSkills(Integer userId) {
        User user = userRepository.findWithSkillsById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return user.getSkills().stream().map(UserMapper::toResponse).toList();
    }

    @Transactional
    public Map<String, String> addSkillToUser(Integer userId, Integer skillId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Skill skill = skillRepository.findById(skillId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Skill not found"));
        if (user.getSkills().contains(skill)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Skill already added");
        }
        user.getSkills().add(skill);
        userRepository.save(user);
        return Map.of("message", "Skill added successfully");
    }

    @Transactional
    public Map<String, String> removeSkillFromUser(Integer userId, Integer skillId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.getSkills().removeIf(s -> s.getId().equals(skillId));
        userRepository.save(user);
        return Map.of("message", "Skill removed successfully");
    }
}
