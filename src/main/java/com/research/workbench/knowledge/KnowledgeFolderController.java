package com.research.workbench.knowledge;

import com.research.workbench.api.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeFolderController {

    private final KnowledgeFolderService knowledgeFolderService;

    public KnowledgeFolderController(KnowledgeFolderService knowledgeFolderService) {
        this.knowledgeFolderService = knowledgeFolderService;
    }

    @GetMapping("/bases/{id}/folders")
    public ApiResponse<List<Map<String, Object>>> list(@PathVariable Long id) {
        return ApiResponse.ok(knowledgeFolderService.list(id));
    }

    @PostMapping("/bases/{id}/folders")
    public ApiResponse<Map<String, Object>> create(@PathVariable Long id, @RequestBody FolderRequest request) {
        return ApiResponse.ok(knowledgeFolderService.create(id, request.name(), request.parentId()));
    }

    @PutMapping("/folders/{id}")
    public ApiResponse<Map<String, Object>> rename(@PathVariable Long id, @RequestBody FolderRequest request) {
        return ApiResponse.ok(knowledgeFolderService.rename(id, request.name()));
    }

    @DeleteMapping("/folders/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        knowledgeFolderService.delete(id);
        return ApiResponse.ok(null);
    }

    public record FolderRequest(String name, Long parentId) {
    }
}
