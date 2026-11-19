package com.qaforge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportProjectRequest {

    @NotBlank(message = "Repository URL is required")
    private String repoUrl;

    private String branch = "main";

    private String language;

    // GITHUB або ARCHIVE
    private String importType = "GITHUB";
}
