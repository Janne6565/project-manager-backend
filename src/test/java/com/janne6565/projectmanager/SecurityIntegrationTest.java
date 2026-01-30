package com.janne6565.projectmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.janne6565.projectmanager.dto.LoginRequest;
import com.janne6565.projectmanager.dto.LoginResponse;
import com.janne6565.projectmanager.entities.Project;
import com.janne6565.projectmanager.repositories.ProjectRepository;
import com.janne6565.projectmanager.util.TestFixtures;
import com.janne6565.projectmanager.config.TestConfig;
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
class SecurityIntegrationTest {

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

    @Test
    void shouldCompleteFullUserFlow() throws Exception {
        // 1. Login
        LoginRequest loginRequest = new LoginRequest(TestFixtures.TEST_USERNAME, TestFixtures.TEST_PASSWORD);
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                LoginResponse.class
        );
        String token = loginResponse.getToken();
        assertThat(token).isNotEmpty();

        // 2. Create a project
        Project newProject = TestFixtures.createTestProject("Integration Test Project", "E2E Test Description");
        MvcResult createResult = mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").exists())
                .andReturn();

        Project createdProject = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                Project.class
        );
        assertThat(createdProject.getUuid()).isNotNull();

        // 3. Get the project (public endpoint, no auth needed)
        mockMvc.perform(get("/projects/" + createdProject.getUuid()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Integration Test Project"))
                .andExpect(jsonPath("$.uuid").value(createdProject.getUuid()));

        // 4. List all projects (public endpoint)
        mockMvc.perform(get("/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));

        // 5. Delete the project
        mockMvc.perform(delete("/projects/" + createdProject.getUuid())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 6. Verify deletion
        mockMvc.perform(get("/projects/" + createdProject.getUuid()))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        assertThat(projectRepository.findById(createdProject.getUuid())).isEmpty();
    }

    @Test
    void shouldHandleMultipleProjectsInSameSession() throws Exception {
        // Login once
        LoginRequest loginRequest = new LoginRequest(TestFixtures.TEST_USERNAME, TestFixtures.TEST_PASSWORD);
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                LoginResponse.class
        ).getToken();

        // Create multiple projects with the same token
        for (int i = 1; i <= 3; i++) {
            Project project = TestFixtures.createTestProject("Project " + i, "Description " + i);
            mockMvc.perform(post("/projects")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(project)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Project " + i));
        }

        // Verify all projects were created
        mockMvc.perform(get("/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void shouldEnforceAuthorizationRulesCorrectly() throws Exception {
        Project project = TestFixtures.createTestProject("Test Project", "Description");

        // 1. GET without auth should work
        mockMvc.perform(get("/projects"))
                .andExpect(status().isOk());

        // 2. POST without auth should fail
        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isForbidden());

        // 3. DELETE without auth should fail
        mockMvc.perform(delete("/projects/some-uuid"))
                .andExpect(status().isForbidden());

        // 4. Login and verify protected endpoints work
        LoginRequest loginRequest = new LoginRequest(TestFixtures.TEST_USERNAME, TestFixtures.TEST_PASSWORD);
        String token = objectMapper.readValue(
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                        .andReturn().getResponse().getContentAsString(),
                LoginResponse.class
        ).getToken();

        // POST with auth should work
        mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectInvalidTokens() throws Exception {
        Project project = TestFixtures.createTestProject("Test Project", "Description");

        // Malformed JWT tokens will cause the filter to throw an exception
        // which results in no authorization being set, so we get 403 Forbidden
        mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer invalid.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isForbidden());

        // Try with empty token - still gets 403 because no valid auth
        mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldHandleConcurrentRequestsWithSameToken() throws Exception {
        // Get a valid token
        LoginRequest loginRequest = new LoginRequest(TestFixtures.TEST_USERNAME, TestFixtures.TEST_PASSWORD);
        String token = objectMapper.readValue(
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                        .andReturn().getResponse().getContentAsString(),
                LoginResponse.class
        ).getToken();

        // Make multiple requests with the same token
        for (int i = 0; i < 5; i++) {
            Project project = TestFixtures.createTestProject("Concurrent Project " + i, "Description " + i);
            mockMvc.perform(post("/projects")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(project)))
                    .andExpect(status().isOk());
        }

        // Verify all were created
        MvcResult result = mockMvc.perform(get("/projects"))
                .andExpect(status().isOk())
                .andReturn();

        Project[] projects = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                Project[].class
        );
        assertThat(projects).hasSize(5);
    }
}
