package com.skillbridge.user.controller;

import com.skillbridge.user.dto.ApiResponse;
import com.skillbridge.user.service.SkillService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    public ApiResponse<?> create(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(skillService.create(body.get("name")));
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

    @DeleteMapping("/me/{skillId}")
    public ApiResponse<?> removeSkill(
        @PathVariable Integer skillId,
        @RequestHeader("x-user-id") Integer userId
    ) {
        return ApiResponse.ok(skillService.removeSkillFromUser(userId, skillId));
    }
}
