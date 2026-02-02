package com.janne6565.projectmanager.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.janne6565.projectmanager.config.TestConfig;
import com.janne6565.projectmanager.dto.LoginRequest;
import com.janne6565.projectmanager.util.TestFixtures;
import jakarta.servlet.http.Cookie;
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
class AuthenticationControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void shouldLoginSuccessfullyWithValidCredentials() throws Exception {
        LoginRequest loginRequest = new LoginRequest(TestFixtures.TEST_USERNAME, TestFixtures.TEST_PASSWORD);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.username").value(TestFixtures.TEST_USERNAME))
                .andExpect(jsonPath("$.expiresIn").value(3600000))
                .andExpect(cookie().exists("JWT-TOKEN"))
                .andExpect(cookie().httpOnly("JWT-TOKEN", true))
                .andExpect(cookie().path("JWT-TOKEN", "/"))
                .andReturn();

        String cookieValue = result.getResponse().getCookie("JWT-TOKEN").getValue();
        assertThat(cookieValue).isNotEmpty();
        
        // JWT should have 3 parts separated by dots
        String[] jwtParts = cookieValue.split("\\.");
        assertThat(jwtParts).hasSize(3);
    }

    @Test
    void shouldReturnForbiddenWithInvalidPassword() throws Exception {
        LoginRequest loginRequest = new LoginRequest(TestFixtures.TEST_USERNAME, TestFixtures.WRONG_PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenWithInvalidUsername() throws Exception {
        LoginRequest loginRequest = new LoginRequest("invaliduser", TestFixtures.TEST_PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnBadRequestWithMissingCredentials() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnValidJwtTokenStructure() throws Exception {
        LoginRequest loginRequest = new LoginRequest(TestFixtures.TEST_USERNAME, TestFixtures.TEST_PASSWORD);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("JWT-TOKEN"))
                .andReturn();

        String token = result.getResponse().getCookie("JWT-TOKEN").getValue();

        // JWT should have 3 parts separated by dots
        String[] jwtParts = token.split("\\.");
        assertThat(jwtParts).hasSize(3);
    }

    @Test
    void shouldLogoutAndClearCookie() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("JWT-TOKEN"))
                .andExpect(cookie().maxAge("JWT-TOKEN", 0));
    }

    @Test
    void shouldReturnAuthenticatedStatusWhenLoggedIn() throws Exception {
        // First login to get cookie
        LoginRequest loginRequest = new LoginRequest(TestFixtures.TEST_USERNAME, TestFixtures.TEST_PASSWORD);
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie jwtCookie = loginResult.getResponse().getCookie("JWT-TOKEN");

        // Check status with valid cookie
        mockMvc.perform(get("/auth/status")
                        .cookie(jwtCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value(TestFixtures.TEST_USERNAME));
    }

    @Test
    void shouldReturnUnauthenticatedStatusWhenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/auth/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.username").doesNotExist());
    }

    @Test
    void shouldReturnUnauthenticatedStatusAfterLogout() throws Exception {
        // Login
        LoginRequest loginRequest = new LoginRequest(TestFixtures.TEST_USERNAME, TestFixtures.TEST_PASSWORD);
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie jwtCookie = loginResult.getResponse().getCookie("JWT-TOKEN");

        // Logout
        mockMvc.perform(post("/auth/logout")
                        .cookie(jwtCookie))
                .andExpect(status().isOk());

        // Check status should be unauthenticated
        mockMvc.perform(get("/auth/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }
}
