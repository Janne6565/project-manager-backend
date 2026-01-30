package com.janne6565.projectmanager.util;

import com.janne6565.projectmanager.entities.Project;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestFixtures {

    public static Project createTestProject(String name, String description) {
        return Project.builder()
                .name(name)
                .description(description)
                .additionalInformation(new HashMap<>())
                .repositories(List.of())
                .build();
    }

    public static Project createTestProjectWithId(String uuid, String name, String description) {
        return Project.builder()
                .uuid(uuid)
                .name(name)
                .description(description)
                .additionalInformation(createTestAdditionalInfo())
                .repositories(List.of("https://github.com/test/repo"))
                .build();
    }

    public static Map<String, String> createTestAdditionalInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("status", "active");
        info.put("priority", "high");
        return info;
    }

    public static final String TEST_USERNAME = "testuser";
    public static final String TEST_PASSWORD = "testpassword";
    public static final String WRONG_PASSWORD = "wrongpassword";
}
