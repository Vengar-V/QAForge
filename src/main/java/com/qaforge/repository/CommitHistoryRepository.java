package com.qaforge.repository;

import com.qaforge.entity.CommitHistory;
import com.qaforge.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommitHistoryRepository extends JpaRepository<CommitHistory, UUID> {

    List<CommitHistory> findByProject(Project project);

    List<CommitHistory> findByProjectAndIsBugFixTrue(Project project);

    boolean existsByProjectAndCommitHash(Project project, String commitHash);

    long countByProject(Project project);

    long countByProjectAndIsBugFixTrue(Project project);
}
