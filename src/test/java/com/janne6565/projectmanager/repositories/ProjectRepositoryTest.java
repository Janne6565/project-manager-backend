package com.janne6565.projectmanager.repositories;

import com.janne6565.projectmanager.dto.external.contributions.ContributionSummaryDto;
import com.janne6565.projectmanager.dto.external.contributions.ContributionTotalsDto;
import com.janne6565.projectmanager.entities.Project;
import com.janne6565.projectmanager.services.external.ExternalContributionService;
import com.janne6565.projectmanager.util.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProjectRepositoryTest {

    @Autowired
    private ProjectRepository projectRepository;

    @MockitoBean
    private ExternalContributionService externalContributionService;

    @BeforeEach
    void setup() {
        ContributionSummaryDto emptySummary = new ContributionSummaryDto(
                java.util.Map.of(), java.util.List.of(), new ContributionTotalsDto(0, 0, 0, 0));
        org.mockito.Mockito.when(externalContributionService.getContributions())
                .thenReturn(Mono.just(emptySummary));
    }

    @Test
    void shouldSaveProject() {
        Project project = TestFixtures.createTestProject("Test Project", "Test Description");

        Project savedProject = projectRepository.save(project);

        assertThat(savedProject).isNotNull();
        assertThat(savedProject.getUuid()).isNotNull();
        assertThat(savedProject.getName()).isEqualTo("Test Project");
        assertThat(savedProject.getDescription()).isEqualTo("Test Description");
    }

    @Test
    void shouldFindAllProjects() {
        projectRepository.save(TestFixtures.createTestProject("Project 1", "Description 1"));
        projectRepository.save(TestFixtures.createTestProject("Project 2", "Description 2"));

        Iterable<Project> projects = projectRepository.findAll();

        assertThat(projects).hasSize(2);
    }

    @Test
    void shouldFindProjectById() {
        Project saved = projectRepository.save(TestFixtures.createTestProject("Test Project", "Description"));

        Optional<Project> found = projectRepository.findById(saved.getUuid());

        assertThat(found).isPresent();
        assertThat(found.get().getUuid()).isEqualTo(saved.getUuid());
        assertThat(found.get().getName()).isEqualTo("Test Project");
    }

    @Test
    void shouldReturnEmptyWhenProjectNotFound() {
        Optional<Project> found = projectRepository.findById("non-existent-uuid");

        assertThat(found).isEmpty();
    }

    @Test
    void shouldDeleteProject() {
        Project saved = projectRepository.save(TestFixtures.createTestProject("Test Project", "Description"));
        String uuid = saved.getUuid();

        projectRepository.deleteById(uuid);

        Optional<Project> found = projectRepository.findById(uuid);
        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindProjectsWithPagination() {
        for (int i = 1; i <= 5; i++) {
            projectRepository.save(TestFixtures.createTestProject("Project " + i, "Description " + i));
        }

        Page<Project> page = projectRepository.findAll(PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }
}
