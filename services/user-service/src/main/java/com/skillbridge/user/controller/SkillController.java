package com.skillbridge.user.controller;

import com.skillbridge.user.dto.ApiResponse;
import com.skillbridge.user.dto.BatchCreateSkillsRequest;
import com.skillbridge.user.dto.CreateSkillRequest;
import com.skillbridge.user.dto.ReplaceUserSkillsRequest;
import com.skillbridge.user.service.SkillService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    public ApiResponse<?> findAll() {
        return ApiResponse.ok(skillService.findAll());
    }

    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody CreateSkillRequest request) {
        return ApiResponse.ok(skillService.create(request));
    }

    @PostMapping("/batch")
    public ApiResponse<?> createBatch(@Valid @RequestBody BatchCreateSkillsRequest request) {
        return ApiResponse.ok(skillService.createBatch(request));
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<?> getUserSkills(@PathVariable Integer userId) {
        return ApiResponse.ok(skillService.getUserSkills(userId));
    }

    @PostMapping("/me/{skillId}")
    public ApiResponse<?> addSkill(
        @PathVariable Integer skillId,
        @RequestHeader("x-user-id") Integer userId
    ) {
        return ApiResponse.ok(skillService.addSkillToUser(userId, skillId));
    }

    @PutMapping("/me")
    public ApiResponse<?> replaceSkills(
        @RequestHeader("x-user-id") Integer userId,
        @Valid @RequestBody ReplaceUserSkillsRequest request
    ) {
        return ApiResponse.ok(skillService.replaceUserSkills(userId, request));
    }

    @DeleteMapping("/me/{skillId}")
    public ApiResponse<?> removeSkill(
        @PathVariable Integer skillId,
        @RequestHeader("x-user-id") Integer userId
    ) {
        return ApiResponse.ok(skillService.removeSkillFromUser(userId, skillId));
    }
}
