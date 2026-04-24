package com.research.workbench.api;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/workbench")
public class WorkbenchController {

    private final WorkbenchDataService workbenchDataService;

    public WorkbenchController(WorkbenchDataService workbenchDataService) {
        this.workbenchDataService = workbenchDataService;
    }

    @GetMapping("/bootstrap")
    public ApiResponse<Map<String, Object>> bootstrap() {
        return ApiResponse.ok(workbenchDataService.bootstrap());
    }

    @PostMapping("/research/query")
    public ApiResponse<Map<String, Object>> queryResearch(@RequestBody SearchRequest request) {
        return ApiResponse.ok(workbenchDataService.searchResearch(request.query()));
    }

    @PostMapping("/web/query")
    public ApiResponse<Map<String, Object>> queryWeb(@RequestBody SearchRequest request) {
        return ApiResponse.ok(workbenchDataService.searchWeb(request.query(), request.platform()));
    }

    @PostMapping("/assistant/chat")
    public ApiResponse<Map<String, Object>> chat(@RequestBody ChatRequest request) {
        return ApiResponse.ok(workbenchDataService.replyAssistant(request.prompt(), request.sources()));
    }

    public record SearchRequest(String query, String platform) {
    }

    public record ChatRequest(@NotBlank String prompt, List<String> sources) {
    }
}
