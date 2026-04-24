package com.research.workbench.websearch;

import com.research.workbench.api.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/websearch")
public class WebFeatureController {

    private final WebFeatureService webFeatureService;

    public WebFeatureController(WebFeatureService webFeatureService) {
        this.webFeatureService = webFeatureService;
    }

    @GetMapping("/history")
    public ApiResponse<List<Map<String, Object>>> history() {
        return ApiResponse.ok(webFeatureService.history());
    }

    @PostMapping("/results/{id}/save")
    public ApiResponse<Map<String, Object>> save(@PathVariable Long id, @RequestBody SaveRequest request) {
        return ApiResponse.ok(webFeatureService.save(id, request.kbId()));
    }

    @PostMapping("/tasks/{id}/save")
    public ApiResponse<Map<String, Object>> saveTask(@PathVariable Long id, @RequestBody SaveRequest request) {
        return ApiResponse.ok(webFeatureService.saveTask(id, request.kbId()));
    }

    public record SaveRequest(Long kbId) {
    }
}
