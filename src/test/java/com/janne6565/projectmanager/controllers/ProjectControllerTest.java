package com.janne6565.projectmanager.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.janne6565.projectmanager.dto.LoginRequest;
import com.janne6565.projectmanager.entities.Project;
import com.janne6565.projectmanager.repositories.ProjectRepository;
import com.janne6565.projectmanager.util.TestFixtures;
import com.janne6565.projectmanager.config.TestConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
class ProjectControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void cleanup() {
        projectRepository.deleteAll();
    }

    private Cookie getJwtCookie() throws Exception {
        LoginRequest loginRequest = new LoginRequest(TestFixtures.TEST_USERNAME, TestFixtures.TEST_PASSWORD);
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return result.getResponse().getCookie("JWT-TOKEN");
    }

    // ========== Public Endpoints Tests ==========

    @Test
    void shouldGetAllProjectsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/projects"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void shouldReturnEmptyListWhenNoProjects() throws Exception {
        mockMvc.perform(get("/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldReturnAllProjectsAfterCreation() throws Exception {
        Cookie jwtCookie = getJwtCookie();

        // Create two projects
        Project project1 = TestFixtures.createTestProject("Project 1", "Description 1");
        mockMvc.perform(post("/projects")
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(project1)))
                .andExpect(status().isOk());

        Project project2 = TestFixtures.createTestProject("Project 2", "Description 2");
        mockMvc.perform(post("/projects")
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(project2)))
                .andExpect(status().isOk());

        // Get all projects
        mockMvc.perform(get("/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[1].name").exists());
    }

    @Test
    void shouldGetProjectByIdWithoutAuthentication() throws Exception {
        Cookie jwtCookie = getJwtCookie();
        Project project = TestFixtures.createTestProject("Test Project", "Test Description");

        // Create project first
        MvcResult createResult = mockMvc.perform(post("/projects")
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isOk())
                .andReturn();

        Project createdProject = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                Project.class
        );

        // Get by ID without auth
        mockMvc.perform(get("/projects/" + createdProject.getUuid()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(createdProject.getUuid()))
                .andExpect(jsonPath("$.name").value("Test Project"))
                .andExpect(jsonPath("$.description").value("Test Description"));
    }

    @Test
    void shouldReturnNullForNonExistentProject() throws Exception {
        mockMvc.perform(get("/projects/non-existent-uuid"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    // ========== Protected Endpoints Tests ==========

    @Test
    void shouldCreateProjectWithValidToken() throws Exception {
        Cookie jwtCookie = getJwtCookie();
        Project project = TestFixtures.createTestProject("New Project", "New Description");

        mockMvc.perform(post("/projects")
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.name").value("New Project"))
                .andExpect(jsonPath("$.description").value("New Description"));
    }

    @Test
    void shouldReturnForbiddenWhenCreatingProjectWithoutToken() throws Exception {
        Project project = TestFixtures.createTestProject("New Project", "Description");

        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnErrorWhenCreatingProjectWithInvalidToken() throws Exception {
        Project project = TestFixtures.createTestProject("New Project", "Description");

        // Malformed JWT tokens will cause the filter to throw an exception
        // which results in no authorization being set, so we get 403 Forbidden
        mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer invalid.token.here")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDeleteProjectWithValidToken() throws Exception {
        Cookie jwtCookie = getJwtCookie();
        Project project = TestFixtures.createTestProject("Project to Delete", "Description");

        // Create project
        MvcResult createResult = mockMvc.perform(post("/projects")
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isOk())
                .andReturn();

        Project createdProject = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                Project.class
        );

        // Delete project
        mockMvc.perform(delete("/projects/" + createdProject.getUuid())
                        .cookie(jwtCookie))
                .andExpect(status().isOk());

        // Verify deletion
        assertThat(projectRepository.findById(createdProject.getUuid())).isEmpty();
    }

    @Test
    void shouldReturnForbiddenWhenDeletingProjectWithoutToken() throws Exception {
        mockMvc.perform(delete("/projects/some-uuid"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnErrorWhenDeletingProjectWithInvalidToken() throws Exception {
        // Malformed JWT tokens will cause the filter to throw an exception
        // which results in no authorization being set, so we get 403 Forbidden
        mockMvc.perform(delete("/projects/some-uuid")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isForbidden());
    }

    // ========== PUT Endpoint Tests ==========

    @Test
    void shouldUpdateProjectWithValidToken() throws Exception {
        Cookie jwtCookie = getJwtCookie();
        
        // Create a project first
        Project project = TestFixtures.createTestProject("Original Project", "Original Description");
        MvcResult createResult = mockMvc.perform(post("/projects")
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isOk())
                .andReturn();

        Project createdProject = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                Project.class
        );

        // Update the project
        Project updateData = TestFixtures.createTestProject("Updated Project", "Updated Description");
        mockMvc.perform(put("/projects/" + createdProject.getUuid())
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(createdProject.getUuid()))
                .andExpect(jsonPath("$.name").value("Updated Project"))
                .andExpect(jsonPath("$.description").value("Updated Description"));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistentProject() throws Exception {
        Cookie jwtCookie = getJwtCookie();
        Project updateData = TestFixtures.createTestProject("Updated Project", "Updated Description");

        mockMvc.perform(put("/projects/non-existent-uuid")
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnForbiddenWhenUpdatingProjectWithoutToken() throws Exception {
        Project updateData = TestFixtures.createTestProject("Updated Project", "Updated Description");

        mockMvc.perform(put("/projects/some-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenWhenUpdatingProjectWithInvalidToken() throws Exception {
        Project updateData = TestFixtures.createTestProject("Updated Project", "Updated Description");

        mockMvc.perform(put("/projects/some-uuid")
                        .header("Authorization", "Bearer invalid.token.here")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldCreateProjectWithAllFields() throws Exception {
        Cookie jwtCookie = getJwtCookie();
        Project project = TestFixtures.createTestProjectWithId(null, "Full Project", "Full Description");

        MvcResult result = mockMvc.perform(post("/projects")
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.name").value("Full Project"))
                .andExpect(jsonPath("$.description").value("Full Description"))
                .andExpect(jsonPath("$.additionalInformation").exists())
                .andExpect(jsonPath("$.repositories").isArray())
                .andReturn();

        Project createdProject = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                Project.class
        );
        assertThat(createdProject.getAdditionalInformation()).containsKey("status");
    }

    @Test
    void shouldUpdateProjectIndex() throws Exception {
        Cookie jwtCookie = getJwtCookie();
        
        // Create a project
        Project project = TestFixtures.createTestProject("Test Project", "Description");
        project.setIndex(0);
        MvcResult createResult = mockMvc.perform(post("/projects")
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isOk())
                .andReturn();

        Project createdProject = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                Project.class
        );

        // Update the index
        mockMvc.perform(patch("/projects/" + createdProject.getUuid() + "/index")
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"index\": 5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.index").value(5))
                .andExpect(jsonPath("$.name").value("Test Project"))
                .andExpect(jsonPath("$.description").value("Description"));

        // Verify the index was updated in the database
        Project updatedProject = projectRepository.findById(createdProject.getUuid()).orElse(null);
        assertThat(updatedProject).isNotNull();
        assertThat(updatedProject.getIndex()).isEqualTo(5);
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingIndexOfNonExistentProject() throws Exception {
        Cookie jwtCookie = getJwtCookie();

        mockMvc.perform(patch("/projects/non-existent-uuid/index")
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"index\": 5}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnForbiddenWhenUpdatingIndexWithoutToken() throws Exception {
        mockMvc.perform(patch("/projects/some-uuid/index")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"index\": 5}"))
                .andExpect(status().isForbidden());
    }
}
