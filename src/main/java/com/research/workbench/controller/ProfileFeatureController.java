package com.research.workbench.controller;

import com.research.workbench.service.ProfileFeatureService;
import com.research.workbench.controller.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
public class ProfileFeatureController {

    private final ProfileFeatureService profileFeatureService;

    public ProfileFeatureController(ProfileFeatureService profileFeatureService) {
        this.profileFeatureService = profileFeatureService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> profile() {
        return ApiResponse.ok(profileFeatureService.profile());
    }

    @PutMapping
    public ApiResponse<Map<String, Object>> update(@RequestBody ProfileFeatureService.ProfileRequest request) {
        return ApiResponse.ok(profileFeatureService.update(request));
    }

    @PostMapping("/avatar/upload")
    public ApiResponse<Map<String, Object>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(profileFeatureService.uploadAvatar(file));
    }

    @GetMapping("/avatar/{filename:.+}")
    public ResponseEntity<Resource> readAvatar(@PathVariable String filename) {
        return profileFeatureService.readAvatar(filename)
                .map(item -> ResponseEntity.ok()
                        .header("Content-Type", item.contentType().toString())
                        .body(item.resource()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/bindings")
    public ApiResponse<List<Map<String, Object>>> bindings() {
        return ApiResponse.ok(profileFeatureService.bindings());
    }

    @PostMapping("/bindings")
    public ApiResponse<Map<String, Object>> bind(@RequestBody BindingRequest request) {
        return ApiResponse.ok(profileFeatureService.bind(request.platform(), request.openId()));
    }

    public record BindingRequest(String platform, String openId) {
    }
}
