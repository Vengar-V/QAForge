package com.qaforge.service;

import com.qaforge.dto.ImportProjectRequest;
import com.qaforge.dto.ProjectDto;
import com.qaforge.entity.Project;
import com.qaforge.entity.User;
import com.qaforge.repository.ProjectRepository;
import com.qaforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<ProjectDto> getAllProjects(String email) {
        User user = getUser(email);
        return projectRepository.findByUserOrderByCreatedAtDesc(user)
                .stream().map(ProjectDto::from).toList();
    }

    public ProjectDto getProject(UUID id, String email) {
        User user = getUser(email);
        return ProjectDto.from(projectRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Project not found")));
    }

    @Transactional
    public ProjectDto importProject(ImportProjectRequest request, String email) {
        User user = getUser(email);

        if (projectRepository.existsByRepoUrlAndUser(request.getRepoUrl(), user)) {
            throw new RuntimeException("Project with this repository already exists");
        }

        String name = extractNameFromUrl(request.getRepoUrl());

        Project project = Project.builder()
                .user(user)
                .name(name)
                .repoUrl(request.getRepoUrl())
                .branch(request.getBranch() != null ? request.getBranch() : "main")
                .language(request.getLanguage())
                .importType(Project.ImportType.valueOf(
                        request.getImportType() != null ? request.getImportType() : "GITHUB"))
                .status(Project.ProjectStatus.PENDING)
                .build();

        Project saved = projectRepository.save(project);
        log.info("Project imported: {} by {}", saved.getName(), email);
        return ProjectDto.from(saved);
    }

    @Transactional
    public void deleteProject(UUID id, String email) {
        User user = getUser(email);
        Project project = projectRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        projectRepository.delete(project);
        log.info("Project deleted: {} by {}", project.getName(), email);
    }

    private String extractNameFromUrl(String url) {
        if (url == null) return "unknown";
        url = url.trim().replaceAll("/$", "").replaceAll("\\.git$", "");
        String[] parts = url.split("/");
        if (parts.length >= 2) {
            return parts[parts.length - 2] + "/" + parts[parts.length - 1];
        }
        return url;
    }
}