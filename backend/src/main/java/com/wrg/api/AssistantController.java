package com.wrg.api;

import com.wrg.api.dto.AssistantDtos.ChatRequest;
import com.wrg.api.dto.AssistantDtos.ChatResponse;
import com.wrg.security.CurrentUser;
import com.wrg.service.AssistantService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assistant")
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
public class AssistantController {
    private final AssistantService assistant;

    public AssistantController(AssistantService assistant) {
        this.assistant = assistant;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return assistant.chat(CurrentUser.require(), request);
    }
}
