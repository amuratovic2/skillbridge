package com.skillbridge.user.repository;

import com.skillbridge.user.model.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Integer> {
    Optional<Skill> findByName(String name);

    @Query("select lower(s.name) from Skill s where lower(s.name) in :names")
    List<String> findExistingNames(@Param("names") Collection<String> normalizedNames);
}
