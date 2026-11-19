package com.qaforge.repository;

import com.qaforge.entity.Project;
import com.qaforge.entity.TestScenario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TestScenarioRepository extends JpaRepository<TestScenario, UUID> {
    List<TestScenario> findByProjectOrderByCreatedAtDesc(Project project);
    List<TestScenario> findByProjectAndFileRisk_IdOrderByCreatedAtDesc(Project project, UUID fileRiskId);
}