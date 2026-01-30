package com.janne6565.projectmanager.repositories;


import com.janne6565.projectmanager.entities.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, String> {
}
