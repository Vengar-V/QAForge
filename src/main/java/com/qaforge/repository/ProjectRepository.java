package com.qaforge.repository;

import com.qaforge.entity.Project;
import com.qaforge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByUserOrderByCreatedAtDesc(User user);

    Optional<Project> findByIdAndUser(UUID id, User user);

    boolean existsByRepoUrlAndUser(String repoUrl, User user);
}
