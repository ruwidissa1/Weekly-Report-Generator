package com.wrg.api;

import com.wrg.api.dto.ProjectDtos.ProjectRequest;
import com.wrg.api.dto.ProjectDtos.ProjectResponse;
import com.wrg.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projects;

    public ProjectController(ProjectService projects) {
        this.projects = projects;
    }

    @GetMapping
    public List<ProjectResponse> list(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return projects.list(includeInactive);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ProjectResponse create(@Valid @RequestBody ProjectRequest request) {
        return projects.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ProjectResponse update(@PathVariable Long id, @Valid @RequestBody ProjectRequest request) {
        return projects.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public void delete(@PathVariable Long id) {
        projects.delete(id);
    }
}
