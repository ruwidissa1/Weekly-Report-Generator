package com.wrg.repository;

import com.wrg.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByActiveTrueOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
