package com.janne6565.projectmanager.entities;

import com.janne6565.projectmanager.dto.external.contributions.ContributionDto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String uuid;
    private Integer index;
    private String name;
    private String description;
    @Column(nullable = false)
    @Builder.Default
    private Boolean isVisible = true;
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> additionalInformation;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> repositories;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<ContributionDto> contributions;

    public Project copy() {
        return Project.builder()
                .uuid(uuid)
                .index(index)
                .name(name)
                .description(description)
                .isVisible(isVisible)
                .additionalInformation(additionalInformation != null ? new HashMap<>(additionalInformation) : new HashMap<>())
                .repositories(repositories != null ? new ArrayList<>(repositories) : new ArrayList())
                .contributions(contributions != null ? new ArrayList<>(contributions) : new ArrayList())
                .build();
    }
}
