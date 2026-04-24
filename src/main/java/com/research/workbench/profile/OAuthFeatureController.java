package com.research.workbench.profile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile/oauth")
public class OAuthFeatureController {

    private final OAuthFeatureService oauthFeatureService;

    public OAuthFeatureController(OAuthFeatureService oauthFeatureService) {
        this.oauthFeatureService = oauthFeatureService;
    }

    @GetMapping("/{platform}/authorize")
    public void authorize(@PathVariable String platform, HttpServletResponse response) throws IOException {
        try {
            response.sendRedirect(oauthFeatureService.buildAuthorizeUrl(platform));
        } catch (Exception ex) {
            redirectResult(response, platform, "error", ex.getMessage());
        }
    }

    @GetMapping("/{platform}/callback")
    public void callback(
            @PathVariable String platform,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription,
            HttpServletResponse response
    ) throws IOException {
        if (error != null) {
            redirectResult(response, platform, "error", errorDescription == null ? error : errorDescription);
            return;
        }
        if (code == null || code.isBlank()) {
            redirectResult(response, platform, "error", "授权回调缺少 code");
            return;
        }
        try {
            response.sendRedirect(oauthFeatureService.handleCallback(platform, code));
        } catch (Exception ex) {
            redirectResult(response, platform, "error", ex.getMessage());
        }
    }

    private void redirectResult(HttpServletResponse response, String platform, String status, String message) throws IOException {
        String safeMessage = java.net.URLEncoder.encode(message == null ? "未知错误" : message, java.nio.charset.StandardCharsets.UTF_8);
        response.sendRedirect("/oauth-result.html?platform=" + platform + "&status=" + status + "&message=" + safeMessage);
    }
}
