package com.qaforge.dto;

import com.qaforge.entity.Project;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDto {

    private UUID id;
    private String name;
    private String repoUrl;
    private String language;
    private String branch;
    private String importType;
    private String status;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime createdAt;

    // Конвертуємо Entity → DTO
    public static ProjectDto from(Project project) {
        return ProjectDto.builder()
                .id(project.getId())
                .name(project.getName())
                .repoUrl(project.getRepoUrl())
                .language(project.getLanguage())
                .branch(project.getBranch())
                .importType(project.getImportType().name())
                .status(project.getStatus().name())
                .lastSyncedAt(project.getLastSyncedAt())
                .createdAt(project.getCreatedAt())
                .build();
    }
}
