package com.janne6565.projectmanager.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    private String name;
    private String description;
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> additionalInformation;
    private List<String> repositories;
}
