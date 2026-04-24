package com.research.workbench.controller;

import com.research.workbench.service.RelaxFeatureService;
import com.research.workbench.controller.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/relax")
public class RelaxFeatureController {

    private final RelaxFeatureService relaxFeatureService;

    public RelaxFeatureController(RelaxFeatureService relaxFeatureService) {
        this.relaxFeatureService = relaxFeatureService;
    }

    @PostMapping("/bazi")
    public ApiResponse<Map<String, Object>> bazi(@RequestBody RelaxRequest request) {
        return ApiResponse.ok(relaxFeatureService.bazi(request.input()));
    }

    @PostMapping("/eat")
    public ApiResponse<Map<String, Object>> eat(@RequestBody RelaxRequest request) {
        return ApiResponse.ok(relaxFeatureService.whatToEat(request.input()));
    }

    public record RelaxRequest(String input) {
    }
}
