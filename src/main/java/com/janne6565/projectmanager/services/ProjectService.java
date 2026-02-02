package com.janne6565.projectmanager.services;

import com.janne6565.projectmanager.dto.external.contributions.ContributionDto;
import com.janne6565.projectmanager.entities.Project;
import com.janne6565.projectmanager.repositories.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ContributionService contributionService;
    private List<ContributionDto> unassignedContributions;

    @Scheduled(fixedDelay = 2, timeUnit = TimeUnit.MINUTES)
    public void updateContributions() {
        List<ContributionDto> contributions = contributionService.getContributions();
        log.info("Fetched: {} contributions", contributions.size());

        List<ContributionDto> newUnassignedContributions = new ArrayList<>();
        for (ContributionDto contribution : contributions) {
            boolean contributionAssigned = false;
            for (Project project : projectRepository.findAll()) {
                if (doesRepositoryBelongToProject(contribution.repositoryUrl(), project)) {
                    contributionAssigned = true;
                    break;
                }
            }
            if (!contributionAssigned) {
                newUnassignedContributions.add(contribution);
            }
        }
        unassignedContributions = newUnassignedContributions;

        for (Project project : projectRepository.findAll()) {
            List<ContributionDto> filteredContributions = contributions.stream()
                    .filter(contribution -> doesRepositoryBelongToProject(contribution.repositoryUrl(), project)).toList();
            Project updatedProject = contributionService.updateProjectContributions(project, filteredContributions);
            updatedProject.setContributions(updatedProject.getContributions().stream()
                    .filter(contribution ->
                            doesRepositoryBelongToProject(contribution.repositoryUrl(), project)).toList());
            projectRepository.save(updatedProject);
        }
    }

    public List<ContributionDto> getUnassignedContributions() {
        return unassignedContributions;
    }

    private boolean doesRepositoryBelongToProject(String repository, Project project) {
        if (project.getRepositories() == null) {
            return false;
        }

        String normalizedRepository = normalizeRepository(repository);
        List<String> normalizedConfiguredRepos = project.getRepositories().stream()
                .map(this::normalizeRepository)
                .toList();

        // First, try exact match (backward compatibility)
        if (normalizedConfiguredRepos.contains(normalizedRepository)) {
            return true;
        }

        // Second, try glob pattern matching
        return normalizedConfiguredRepos.stream()
                .filter(configuredRepo -> configuredRepo.contains("*"))
                .anyMatch(pattern -> matchesGlobPattern(normalizedRepository, pattern));
    }

    private boolean matchesGlobPattern(String repository, String globPattern) {
        String regex = convertGlobToRegex(globPattern);
        return Pattern.matches(regex, repository);
    }

    private String convertGlobToRegex(String globPattern) {
        StringBuilder regex = new StringBuilder("^");
        for (char c : globPattern.toCharArray()) {
            if (c == '*') {
                regex.append(".*");
            } else if ("[](){}+.^$|\\?".indexOf(c) != -1) {
                regex.append('\\').append(c);
            } else {
                regex.append(c);
            }
        }
        regex.append("$");
        return regex.toString();
    }

    private String normalizeRepository(String repository) {
        String toLower = repository.toLowerCase();
        String trimmed = toLower.trim();
        String withoutHttp = trimmed.replaceFirst("^https?://", "");
        String withoutTrailingSlash = withoutHttp.replaceAll("/$", "");
        return withoutTrailingSlash;
    }

    public Project createProject(Project project) {
        project.setUuid(null);
        project.setIndex((int) projectRepository.count() + 1);
        if (project.getIsVisible() == null) {
            project.setIsVisible(true);
        }
        Project createdProject = projectRepository.save(project);
        updateContributions();
        return createdProject;
    }

    public Project updateProject(String uuid, Project project) {
        Project newProject = projectRepository.findById(uuid)
                .map(existingProject -> {
                    existingProject.setName(project.getName());
                    existingProject.setDescription(project.getDescription());
                    existingProject.setAdditionalInformation(project.getAdditionalInformation());
                    existingProject.setRepositories(project.getRepositories());
                    if (project.getIsVisible() != null) {
                        existingProject.setIsVisible(project.getIsVisible());
                    }
                    return projectRepository.save(existingProject);
                })
                .orElse(null);
        updateContributions();
        return newProject;
    }

    public Project toggleProjectVisibility(String uuid) {
        return projectRepository.findById(uuid)
                .map(project -> {
                    project.setIsVisible(!Boolean.TRUE.equals(project.getIsVisible()));
                    return projectRepository.save(project);
                })
                .orElse(null);
    }

    public Iterable<Project> getProjects() {
        return projectRepository.findAll().stream()
                .filter(project -> Boolean.TRUE.equals(project.getIsVisible()))
                .toList();
    }

    public Iterable<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    public Project getProjectById(String id) {
        return projectRepository.findById(id).orElse(null);
    }

    public Page<Project> getPagesProjects(PageRequest pageRequest) {
        return projectRepository.findAll(pageRequest);
    }

    public void deleteProject(String uuid) {
        projectRepository.deleteById(uuid);
    }

    public Project updateProjectIndex(String uuid, int index) {
        return projectRepository.findById(uuid)
                .map(project -> {
                    project.setIndex(index);
                    return projectRepository.save(project);
                })
                .orElse(null);
    }
}
