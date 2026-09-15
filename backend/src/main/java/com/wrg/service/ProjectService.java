package com.wrg.service;

import com.wrg.api.dto.ProjectDtos.ProjectRequest;
import com.wrg.api.dto.ProjectDtos.ProjectResponse;
import com.wrg.domain.Project;
import com.wrg.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {
    private final ProjectRepository projects;

    public ProjectService(ProjectRepository projects) {
        this.projects = projects;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list(boolean includeInactive) {
        return (includeInactive ? projects.findAll() : projects.findByActiveTrueOrderByNameAsc())
                .stream()
                .map(ProjectService::toResponse)
                .toList();
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        if (projects.existsByNameIgnoreCase(request.name())) {
            throw new IllegalArgumentException("Project name already exists");
        }
        Project project = new Project();
        project.setName(request.name());
        project.setDescription(request.description());
        if (request.active() != null) {
            project.setActive(request.active());
        }
        return toResponse(projects.save(project));
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = projects.findById(id).orElseThrow(() -> new EntityNotFoundException("Project not found"));
        project.setName(request.name());
        project.setDescription(request.description());
        if (request.active() != null) {
            project.setActive(request.active());
        }
        return toResponse(project);
    }

    @Transactional
    public void delete(Long id) {
        Project project = projects.findById(id).orElseThrow(() -> new EntityNotFoundException("Project not found"));
        project.setActive(false);
    }

    public static ProjectResponse toResponse(Project project) {
        return new ProjectResponse(project.getId(), project.getName(), project.getDescription(), project.isActive());
    }
}
