package com.qaforge.repository;

import com.qaforge.entity.BugReport;
import com.qaforge.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BugReportRepository extends JpaRepository<BugReport, UUID> {
    List<BugReport> findByProjectOrderByCreatedAtDesc(Project project);
}
