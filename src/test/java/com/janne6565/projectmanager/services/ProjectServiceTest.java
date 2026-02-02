package com.janne6565.projectmanager.services;

import com.janne6565.projectmanager.dto.external.contributions.ContributionDto;
import com.janne6565.projectmanager.entities.Project;
import com.janne6565.projectmanager.repositories.ProjectRepository;
import com.janne6565.projectmanager.util.TestFixtures;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ContributionService contributionService;

    @Mock
    private List<ContributionDto> unassignedContributions;

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
    void shouldUpdateProject() {
        Project existingProject = TestFixtures.createTestProjectWithId("test-uuid", "Old Name", "Old Description");
        existingProject.setContributions(new ArrayList<>());
        Project updateData = TestFixtures.createTestProject("New Name", "New Description");
        Project updatedProject = TestFixtures.createTestProjectWithId("test-uuid", "New Name", "New Description");
        updatedProject.setContributions(new ArrayList<>());
        
        when(projectRepository.findById("test-uuid")).thenReturn(Optional.of(existingProject));
        when(projectRepository.save(any(Project.class))).thenReturn(updatedProject);
        when(projectRepository.findAll()).thenReturn(List.of(updatedProject));
        when(contributionService.getContributions()).thenReturn(List.of());
        when(contributionService.updateProjectContributions(any(), any())).thenReturn(updatedProject);

        Project result = projectService.updateProject("test-uuid", updateData);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getDescription()).isEqualTo("New Description");
        verify(projectRepository, times(1)).findById("test-uuid");
    }

    @Test
    void shouldReturnNullWhenUpdatingNonExistentProject() {
        Project updateData = TestFixtures.createTestProject("New Name", "New Description");
        when(projectRepository.findById("non-existent")).thenReturn(Optional.empty());
        when(projectRepository.findAll()).thenReturn(List.of());
        when(contributionService.getContributions()).thenReturn(List.of());

        Project result = projectService.updateProject("non-existent", updateData);

        assertThat(result).isNull();
        verify(projectRepository, times(1)).findById("non-existent");
        verify(projectRepository, never()).save(any(Project.class));
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

    @Nested
    class RepositoryPatternMatchingTests {
        
        @Test
        void shouldMatchExactRepositoryUrl() throws Exception {
            Project project = Project.builder()
                    .repositories(List.of("github.com/janne6565/projectmanager"))
                    .build();
            
            boolean result = invokeDoesRepositoryBelongToProject("https://github.com/janne6565/projectmanager", project);
            
            assertThat(result).isTrue();
        }
        
        @Test
        void shouldMatchExactRepositoryUrlWithDifferentCasing() throws Exception {
            Project project = Project.builder()
                    .repositories(List.of("github.com/janne6565/ProjectManager"))
                    .build();
            
            boolean result = invokeDoesRepositoryBelongToProject("https://GitHub.com/Janne6565/projectmanager/", project);
            
            assertThat(result).isTrue();
        }
        
        @Test
        void shouldMatchSingleWildcardPattern() throws Exception {
            Project project = Project.builder()
                    .repositories(List.of("github.com/janne6565/*"))
                    .build();
            
            boolean result1 = invokeDoesRepositoryBelongToProject("https://github.com/janne6565/projectmanager", project);
            boolean result2 = invokeDoesRepositoryBelongToProject("https://github.com/janne6565/another-repo", project);
            boolean result3 = invokeDoesRepositoryBelongToProject("https://github.com/otheruser/projectmanager", project);
            
            assertThat(result1).isTrue();
            assertThat(result2).isTrue();
            assertThat(result3).isFalse();
        }
        
        @Test
        void shouldMatchMultipleWildcards() throws Exception {
            Project project = Project.builder()
                    .repositories(List.of("github.com/*/projectmanager*"))
                    .build();
            
            boolean result1 = invokeDoesRepositoryBelongToProject("https://github.com/janne6565/projectmanager", project);
            boolean result2 = invokeDoesRepositoryBelongToProject("https://github.com/otheruser/projectmanager", project);
            boolean result3 = invokeDoesRepositoryBelongToProject("https://github.com/someone/projectmanager-backend", project);
            boolean result4 = invokeDoesRepositoryBelongToProject("https://github.com/janne6565/different-repo", project);
            
            assertThat(result1).isTrue();
            assertThat(result2).isTrue();
            assertThat(result3).isTrue();
            assertThat(result4).isFalse();
        }
        
        @Test
        void shouldMatchWildcardPrefix() throws Exception {
            Project project = Project.builder()
                    .repositories(List.of("github.com/janne6565/project*"))
                    .build();
            
            boolean result1 = invokeDoesRepositoryBelongToProject("https://github.com/janne6565/project", project);
            boolean result2 = invokeDoesRepositoryBelongToProject("https://github.com/janne6565/project1", project);
            boolean result3 = invokeDoesRepositoryBelongToProject("https://github.com/janne6565/projectmanager", project);
            boolean result4 = invokeDoesRepositoryBelongToProject("https://github.com/janne6565/myproject", project);
            
            assertThat(result1).isTrue();
            assertThat(result2).isTrue();
            assertThat(result3).isTrue();
            assertThat(result4).isFalse();
        }
        
        @Test
        void shouldNotMatchWhenNoRepositoriesConfigured() throws Exception {
            Project project = Project.builder()
                    .repositories(null)
                    .build();
            
            boolean result = invokeDoesRepositoryBelongToProject("https://github.com/janne6565/projectmanager", project);
            
            assertThat(result).isFalse();
        }
        
        @Test
        void shouldNotMatchWhenRepositoryListIsEmpty() throws Exception {
            Project project = Project.builder()
                    .repositories(new ArrayList<>())
                    .build();
            
            boolean result = invokeDoesRepositoryBelongToProject("https://github.com/janne6565/projectmanager", project);
            
            assertThat(result).isFalse();
        }
        
        @Test
        void shouldMatchWildcardInMiddle() throws Exception {
            Project project = Project.builder()
                    .repositories(List.of("github.com/*/backend"))
                    .build();
            
            boolean result1 = invokeDoesRepositoryBelongToProject("https://github.com/janne6565/backend", project);
            boolean result2 = invokeDoesRepositoryBelongToProject("https://github.com/otheruser/backend", project);
            boolean result3 = invokeDoesRepositoryBelongToProject("https://github.com/janne6565/frontend", project);
            
            assertThat(result1).isTrue();
            assertThat(result2).isTrue();
            assertThat(result3).isFalse();
        }
        
        @Test
        void shouldHandleSpecialRegexCharactersInPattern() throws Exception {
            Project project = Project.builder()
                    .repositories(List.of("github.com/janne6565/project.manager*"))
                    .build();
            
            boolean result1 = invokeDoesRepositoryBelongToProject("https://github.com/janne6565/project.manager", project);
            boolean result2 = invokeDoesRepositoryBelongToProject("https://github.com/janne6565/project.manager-v2", project);
            boolean result3 = invokeDoesRepositoryBelongToProject("https://github.com/janne6565/projectXmanager", project);
            
            assertThat(result1).isTrue();
            assertThat(result2).isTrue();
            assertThat(result3).isFalse();
        }
        
        @Test
        void shouldPreferExactMatchOverPattern() throws Exception {
            Project project = Project.builder()
                    .repositories(List.of("github.com/janne6565/*", "github.com/janne6565/projectmanager"))
                    .build();
            
            boolean result = invokeDoesRepositoryBelongToProject("https://github.com/janne6565/projectmanager", project);
            
            assertThat(result).isTrue();
        }
        
        private boolean invokeDoesRepositoryBelongToProject(String repository, Project project) throws Exception {
            Method method = ProjectService.class.getDeclaredMethod("doesRepositoryBelongToProject", String.class, Project.class);
            method.setAccessible(true);
            return (boolean) method.invoke(projectService, repository, project);
        }
    }
}
