package com.qaforge.repository;

import com.qaforge.entity.FileRisk;
import com.qaforge.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FileRiskRepository extends JpaRepository<FileRisk, UUID> {

    List<FileRisk> findByProjectOrderByRiskScoreDesc(Project project);

    List<FileRisk> findByProjectAndRiskLevel(Project project, FileRisk.RiskLevel riskLevel);

    void deleteByProject(Project project);
}
