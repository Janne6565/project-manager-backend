package com.janne6565.projectmanager.services;

import com.janne6565.projectmanager.entities.Project;
import com.janne6565.projectmanager.repositories.ProjectRepository;
import com.janne6565.projectmanager.util.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void shouldCreateProject() {
        Project project = TestFixtures.createTestProject("New Project", "Description");
        Project savedProject = TestFixtures.createTestProjectWithId("test-uuid", "New Project", "Description");
        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);

        Project result = projectService.createProject(project);

        assertThat(result).isNotNull();
        assertThat(result.getUuid()).isEqualTo("test-uuid");
        assertThat(result.getName()).isEqualTo("New Project");
        verify(projectRepository, times(1)).save(project);
    }

    @Test
    void shouldGetAllProjects() {
        List<Project> projects = List.of(
                TestFixtures.createTestProjectWithId("uuid1", "Project 1", "Desc 1"),
                TestFixtures.createTestProjectWithId("uuid2", "Project 2", "Desc 2")
        );
        when(projectRepository.findAll()).thenReturn(projects);

        Iterable<Project> result = projectService.getProjects();

        assertThat(result).hasSize(2);
        verify(projectRepository, times(1)).findAll();
    }

    @Test
    void shouldGetProjectById() {
        Project project = TestFixtures.createTestProjectWithId("test-uuid", "Test Project", "Description");
        when(projectRepository.findById("test-uuid")).thenReturn(Optional.of(project));

        Project result = projectService.getProjectById("test-uuid");

        assertThat(result).isNotNull();
        assertThat(result.getUuid()).isEqualTo("test-uuid");
        assertThat(result.getName()).isEqualTo("Test Project");
        verify(projectRepository, times(1)).findById("test-uuid");
    }

    @Test
    void shouldReturnNullWhenProjectNotFound() {
        when(projectRepository.findById("non-existent")).thenReturn(Optional.empty());

        Project result = projectService.getProjectById("non-existent");

        assertThat(result).isNull();
        verify(projectRepository, times(1)).findById("non-existent");
    }

    @Test
    void shouldDeleteProject() {
        doNothing().when(projectRepository).deleteById("test-uuid");

        projectService.deleteProject("test-uuid");

        verify(projectRepository, times(1)).deleteById("test-uuid");
    }

    @Test
    void shouldGetProjectsWithPagination() {
        List<Project> projects = List.of(
                TestFixtures.createTestProjectWithId("uuid1", "Project 1", "Desc 1"),
                TestFixtures.createTestProjectWithId("uuid2", "Project 2", "Desc 2")
        );
        Page<Project> page = new PageImpl<>(projects, PageRequest.of(0, 2), 5);
        when(projectRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<Project> result = projectService.getPagesProjects(PageRequest.of(0, 2));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(5);
        verify(projectRepository, times(1)).findAll(any(PageRequest.class));
    }
}
