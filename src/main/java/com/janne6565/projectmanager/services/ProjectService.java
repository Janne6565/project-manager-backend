package com.janne6565.projectmanager.services;

import com.janne6565.projectmanager.entities.Project;
import com.janne6565.projectmanager.repositories.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    public Project createProject(Project project) {
        return projectRepository.save(project);
    }

    public Project updateProject(String uuid, Project project) {
        return projectRepository.findById(uuid)
                .map(existingProject -> {
                    existingProject.setName(project.getName());
                    existingProject.setDescription(project.getDescription());
                    existingProject.setAdditionalInformation(project.getAdditionalInformation());
                    existingProject.setRepositories(project.getRepositories());
                    return projectRepository.save(existingProject);
                })
                .orElse(null);
    }

    public Iterable<Project> getProjects() {
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
}
