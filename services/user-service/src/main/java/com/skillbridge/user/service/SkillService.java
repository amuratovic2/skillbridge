package com.skillbridge.user.service;

import com.skillbridge.user.dto.BatchCreateSkillsRequest;
import com.skillbridge.user.dto.CreateSkillRequest;
import com.skillbridge.user.dto.ReplaceUserSkillsRequest;
import com.skillbridge.user.dto.SkillResponse;
import com.skillbridge.user.mapper.UserMapper;
import com.skillbridge.user.model.Skill;
import com.skillbridge.user.model.User;
import com.skillbridge.user.repository.SkillRepository;
import com.skillbridge.user.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        return skillRepository.findAll(Sort.by("name").ascending()).stream().map(UserMapper::toResponse).toList();
    }

    @Transactional
    public SkillResponse create(CreateSkillRequest request) {
        String name = request.name().trim();
        if (!skillRepository.findExistingNames(Set.of(name.toLowerCase())).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Skill already exists");
        }
        Skill skill = new Skill();
        skill.setName(name);
        return UserMapper.toResponse(skillRepository.save(skill));
    }

    @Transactional
    public List<SkillResponse> createBatch(BatchCreateSkillsRequest request) {
        List<String> names = request.skills().stream()
            .map(CreateSkillRequest::name)
            .map(String::trim)
            .toList();

        Set<String> normalized = new LinkedHashSet<>();
        for (String name : names) {
            if (!normalized.add(name.toLowerCase())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Batch contains duplicate skill: " + name);
            }
        }

        List<String> existing = skillRepository.findExistingNames(normalized);
        if (!existing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Skills already exist: " + String.join(", ", existing));
        }

        List<Skill> skills = names.stream()
            .map(name -> {
                Skill skill = new Skill();
                skill.setName(name);
                return skill;
            })
            .toList();

        return skillRepository.saveAll(skills).stream()
            .map(UserMapper::toResponse)
            .toList();
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
    public List<SkillResponse> replaceUserSkills(Integer userId, ReplaceUserSkillsRequest request) {
        User user = userRepository.findWithSkillsById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Set<Integer> requestedIds = new LinkedHashSet<>(request.skillIds());
        List<Skill> skills = skillRepository.findAllById(requestedIds);
        Set<Integer> foundIds = skills.stream().map(Skill::getId).collect(Collectors.toSet());
        List<Integer> missingIds = requestedIds.stream()
            .filter(id -> !foundIds.contains(id))
            .toList();
        if (!missingIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Skills not found: " + missingIds);
        }

        user.getSkills().clear();
        user.getSkills().addAll(skills);
        userRepository.save(user);
        return user.getSkills().stream().map(UserMapper::toResponse).toList();
    }

    @Transactional
    public Map<String, String> removeSkillFromUser(Integer userId, Integer skillId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        boolean removed = user.getSkills().removeIf(s -> s.getId().equals(skillId));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Skill not assigned to user");
        }
        userRepository.save(user);
        return Map.of("message", "Skill removed successfully");
    }
}
