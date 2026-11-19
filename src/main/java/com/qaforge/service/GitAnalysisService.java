package com.qaforge.service;

import com.qaforge.entity.CommitHistory;
import com.qaforge.entity.FileRisk;
import com.qaforge.entity.Project;
import com.qaforge.repository.CommitHistoryRepository;
import com.qaforge.repository.FileRiskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitAnalysisService {

    private final CommitHistoryRepository commitHistoryRepository;
    private final FileRiskRepository fileRiskRepository;

    // Патерни для визначення bug-fix комітів
    private static final Pattern BUG_FIX_PATTERN = Pattern.compile(
        "(?i)(fix|bug|patch|hotfix|resolve|repair|correct|defect|issue|error|fail|crash|broken)",
        Pattern.CASE_INSENSITIVE
    );

    // Директорія для клонування репозиторіїв
    private static final String CLONE_BASE_DIR = System.getProperty("java.io.tmpdir") + "/qaforge-repos/";

    @Transactional
    public void analyzeProject(Project project) {
        log.info("Starting git analysis for project: {}", project.getName());

        File repoDir = new File(CLONE_BASE_DIR + project.getId());

        try {
            Git git = cloneOrOpenRepo(project, repoDir);
            parseCommitHistory(git, project);
            calculateFileRisks(project);
            git.close();
            log.info("Git analysis completed for project: {}", project.getName());
        } catch (Exception e) {
            log.error("Git analysis failed for project {}: {}", project.getName(), e.getMessage());
            throw new RuntimeException("Git analysis failed: " + e.getMessage());
        }
    }

    // Клонуємо або відкриваємо існуючий репозиторій
    private Git cloneOrOpenRepo(Project project, File repoDir) throws GitAPIException, IOException {
        if (repoDir.exists() && repoDir.list() != null && repoDir.list().length > 0) {
            log.info("Opening existing repo at: {}", repoDir.getPath());
            return Git.open(repoDir);
        }

        log.info("Cloning repo: {} to {}", project.getRepoUrl(), repoDir.getPath());
        repoDir.mkdirs();

        return Git.cloneRepository()
                .setURI(project.getRepoUrl())
                .setDirectory(repoDir)
                .setBranch(project.getBranch() != null ? project.getBranch() : "main")
                .setDepth(500) // Беремо останні 500 комітів
                .call();
    }

    // Парсимо git history і зберігаємо в БД
    private void parseCommitHistory(Git git, Project project) throws GitAPIException, IOException {
        Repository repository = git.getRepository();
        Iterable<RevCommit> commits = git.log().setMaxCount(500).call();

        int saved = 0;
        for (RevCommit commit : commits) {
            // Пропускаємо якщо вже є в БД
            if (commitHistoryRepository.existsByProjectAndCommitHash(project, commit.getName())) {
                continue;
            }

            String message = commit.getFullMessage();
            boolean isBugFix = BUG_FIX_PATTERN.matcher(message).find();

            List<String> filesChanged = getChangedFiles(git, repository, commit);

            LocalDateTime committedAt = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(commit.getCommitTime()),
                ZoneId.systemDefault()
            );

            CommitHistory commitHistory = CommitHistory.builder()
                    .project(project)
                    .commitHash(commit.getName())
                    .message(message.trim())
                    .author(commit.getAuthorIdent().getName())
                    .isBugFix(isBugFix)
                    .filesChanged(filesChanged)
                    .committedAt(committedAt)
                    .build();

            commitHistoryRepository.save(commitHistory);
            saved++;
        }

        log.info("Saved {} commits for project: {}", saved, project.getName());
    }

    // Отримуємо список змінених файлів в коміті
    private List<String> getChangedFiles(Git git, Repository repository, RevCommit commit) {
        List<String> files = new ArrayList<>();
        try {
            if (commit.getParentCount() == 0) return files;

            RevCommit parent = commit.getParent(0);
            ObjectReader reader = repository.newObjectReader();

            CanonicalTreeParser oldTree = new CanonicalTreeParser();
            oldTree.reset(reader, parent.getTree());

            CanonicalTreeParser newTree = new CanonicalTreeParser();
            newTree.reset(reader, commit.getTree());

            DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
            diffFormatter.setRepository(repository);
            List<DiffEntry> diffs = diffFormatter.scan(oldTree, newTree);

            for (DiffEntry diff : diffs) {
                String path = diff.getChangeType() == DiffEntry.ChangeType.DELETE
                        ? diff.getOldPath()
                        : diff.getNewPath();
                if (!path.equals("/dev/null")) {
                    files.add(path);
                }
            }
            diffFormatter.close();
        } catch (Exception e) {
            log.warn("Could not get changed files for commit {}: {}", commit.getName(), e.getMessage());
        }
        return files;
    }

    // Розраховуємо risk score для кожного файлу
    @Transactional
    public void calculateFileRisks(Project project) {
        log.info("Calculating file risks for project: {}", project.getName());

        List<CommitHistory> allCommits = commitHistoryRepository.findByProject(project);
        Map<String, Integer> bugFixCount = new HashMap<>();
        Map<String, Integer> commitCount = new HashMap<>();

        // Рахуємо кількість bug-fix і всіх комітів по кожному файлу
        for (CommitHistory commit : allCommits) {
            if (commit.getFilesChanged() == null) continue;
            for (String file : commit.getFilesChanged()) {
                commitCount.merge(file, 1, Integer::sum);
                if (commit.isBugFix()) {
                    bugFixCount.merge(file, 1, Integer::sum);
                }
            }
        }

        // Видаляємо старі ризики
        fileRiskRepository.deleteByProject(project);

        // Зберігаємо нові
        int maxBugs = bugFixCount.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        int maxCommits = commitCount.values().stream().mapToInt(Integer::intValue).max().orElse(1);

        for (String filePath : commitCount.keySet()) {
            int bugs = bugFixCount.getOrDefault(filePath, 0);
            int commits = commitCount.getOrDefault(filePath, 0);

            // Risk score = 60% bug density + 40% change frequency (0-10 шкала)
            double bugScore = (double) bugs / maxBugs * 6.0;
            double changeScore = (double) commits / maxCommits * 4.0;
            double riskScore = bugScore + changeScore;

            FileRisk.RiskLevel level = riskScore >= 7.0
                    ? FileRisk.RiskLevel.HIGH
                    : riskScore >= 4.0
                    ? FileRisk.RiskLevel.MEDIUM
                    : FileRisk.RiskLevel.LOW;

            FileRisk fileRisk = FileRisk.builder()
                    .project(project)
                    .filePath(filePath)
                    .riskScore(BigDecimal.valueOf(riskScore).setScale(2, RoundingMode.HALF_UP))
                    .bugCount(bugs)
                    .commitCount(commits)
                    .changeCount(commits)
                    .riskLevel(level)
                    .calculatedAt(LocalDateTime.now())
                    .build();

            fileRiskRepository.save(fileRisk);
        }

        log.info("Calculated risks for {} files in project: {}",
                commitCount.size(), project.getName());
    }
}
