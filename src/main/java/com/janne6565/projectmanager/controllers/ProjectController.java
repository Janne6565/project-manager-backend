package com.janne6565.projectmanager.controllers;

import com.janne6565.projectmanager.dto.UpdateIndexRequest;
import com.janne6565.projectmanager.entities.Project;
import com.janne6565.projectmanager.services.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<List<Project>> getProjects(@RequestParam(required = false, defaultValue = "true") boolean includeContributions) {
        List<Project> projects = projectService.getProjects().stream().peek(project -> {
            if (!includeContributions) project.setContributions(null);
        }).toList();

        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<Project> getProjectById(@PathVariable String uuid) {
        return ResponseEntity.ok(projectService.getProjectById(uuid));
    }

    @PostMapping
    public ResponseEntity<Project> createProject(@RequestBody Project project) {
        return ResponseEntity.ok(projectService.createProject(project));
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<Project> updateProject(@PathVariable String uuid, @RequestBody Project project) {
        Project updatedProject = projectService.updateProject(uuid, project);
        if (updatedProject == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updatedProject);
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteProject(@PathVariable String uuid) {
        projectService.deleteProject(uuid);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{uuid}/index")
    public ResponseEntity<Project> updateProjectIndex(@PathVariable String uuid, @RequestBody UpdateIndexRequest request) {
        Project updatedProject = projectService.updateProjectIndex(uuid, request.getIndex());
        if (updatedProject == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updatedProject);
    }

    @PatchMapping("/{uuid}/visibility")
    public ResponseEntity<Project> toggleProjectVisibility(@PathVariable String uuid) {
        Project updatedProject = projectService.toggleProjectVisibility(uuid);
        if (updatedProject == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updatedProject);
    }
}
