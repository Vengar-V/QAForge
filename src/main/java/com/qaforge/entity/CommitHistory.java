package com.qaforge.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "commit_history",
    uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "commit_hash"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommitHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "commit_hash", nullable = false, length = 40)
    private String commitHash;

    @Column(columnDefinition = "text")
    private String message;

    private String author;

    @Column(name = "is_bug_fix", nullable = false)
    private boolean isBugFix = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "files_changed", columnDefinition = "jsonb")
    private List<String> filesChanged;

    @Column(name = "committed_at", nullable = false)
    private LocalDateTime committedAt;
}
