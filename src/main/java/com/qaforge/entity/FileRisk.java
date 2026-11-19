package com.qaforge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "file_risks",
    uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "file_path"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FileRisk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "risk_score", precision = 4, scale = 2)
    private BigDecimal riskScore = BigDecimal.ZERO;

    @Column(name = "bug_count")
    private int bugCount = 0;

    @Column(name = "commit_count")
    private int commitCount = 0;

    @Column(name = "coverage_pct", precision = 5, scale = 2)
    private BigDecimal coveragePct;

    @Column(name = "change_count")
    private int changeCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, columnDefinition = "VARCHAR(50)")
    private RiskLevel riskLevel = RiskLevel.LOW;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt = LocalDateTime.now();

    public enum RiskLevel { LOW, MEDIUM, HIGH }
}
