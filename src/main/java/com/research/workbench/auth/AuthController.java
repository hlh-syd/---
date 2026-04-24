package com.research.workbench.auth;

import com.research.workbench.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return ApiResponse.ok(authService.login(request.identifier(), request.password(), httpRequest, httpResponse));
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody RegisterRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return ApiResponse.ok(authService.register(request.username(), request.email(), request.nickname(), request.password(), httpRequest, httpResponse));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        return ApiResponse.ok(authService.me());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ApiResponse.ok(null);
    }

    public record LoginRequest(@NotBlank String identifier, @NotBlank String password) {
    }

    public record RegisterRequest(@NotBlank String username, String email, @NotBlank String nickname, @NotBlank String password) {
    }
}
