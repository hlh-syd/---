package com.research.workbench.controller;

import com.research.workbench.service.AiFeatureService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiFeatureController {

    private final AiFeatureService aiFeatureService;

    public AiFeatureController(AiFeatureService aiFeatureService) {
        this.aiFeatureService = aiFeatureService;
    }

    @GetMapping("/tools")
    public ApiResponse<List<Map<String, Object>>> tools() {
        return ApiResponse.ok(aiFeatureService.toolList());
    }

    @PostMapping("/prompt-optimize")
    public ApiResponse<Map<String, Object>> promptOptimize(@RequestBody PromptRequest request) {
        return ApiResponse.ok(aiFeatureService.optimizePrompt(request.prompt()));
    }

    @PostMapping("/bio-assistant")
    public ApiResponse<Map<String, Object>> bioAssistant(@RequestBody PromptRequest request) {
        return ApiResponse.ok(aiFeatureService.bioAssistant(request.prompt()));
    }

    public record PromptRequest(String prompt) {
    }
}
