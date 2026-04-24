package com.research.workbench.controller;

import com.research.workbench.service.ResearchFeatureService;
import com.research.workbench.controller.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/research")
public class ResearchFeatureController {

    private final ResearchFeatureService researchFeatureService;

    public ResearchFeatureController(ResearchFeatureService researchFeatureService) {
        this.researchFeatureService = researchFeatureService;
    }

    @GetMapping("/history")
    public ApiResponse<List<Map<String, Object>>> history() {
        return ApiResponse.ok(researchFeatureService.history());
    }

    @GetMapping("/knowledge-bases")
    public ApiResponse<List<Map<String, Object>>> knowledgeBases() {
        return ApiResponse.ok(researchFeatureService.knowledgeBases());
    }

    @PostMapping("/results/{id}/summary")
    public ApiResponse<Map<String, Object>> summary(@PathVariable Long id) {
        return ApiResponse.ok(researchFeatureService.summary(id));
    }

    @PostMapping("/results/{id}/mindmap")
    public ApiResponse<Map<String, Object>> mindmap(@PathVariable Long id) {
        return ApiResponse.ok(researchFeatureService.mindmap(id));
    }

    @PostMapping("/results/{id}/save")
    public ApiResponse<Map<String, Object>> save(@PathVariable Long id, @RequestBody SaveRequest request) {
        return ApiResponse.ok(researchFeatureService.saveToKnowledge(id, request.kbId()));
    }

    @PostMapping("/tasks/{id}/save")
    public ApiResponse<Map<String, Object>> saveTask(@PathVariable Long id, @RequestBody SaveRequest request) {
        return ApiResponse.ok(researchFeatureService.saveTaskToKnowledge(id, request.kbId()));
    }

    public record SaveRequest(Long kbId) {
    }
}
