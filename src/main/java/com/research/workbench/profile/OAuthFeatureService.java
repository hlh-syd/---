package com.research.workbench.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.research.workbench.auth.CurrentUserService;
import com.research.workbench.config.AppProperties;
import com.research.workbench.domain.UserSocialBinding;
import com.research.workbench.repository.UserSocialBindingRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
public class OAuthFeatureService {

    private final AppProperties appProperties;
    private final UserSocialBindingRepository userSocialBindingRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final CurrentUserService currentUserService;

    public OAuthFeatureService(
            AppProperties appProperties,
            UserSocialBindingRepository userSocialBindingRepository,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            CurrentUserService currentUserService
    ) {
        this.appProperties = appProperties;
        this.userSocialBindingRepository = userSocialBindingRepository;
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.currentUserService = currentUserService;
    }

    public String buildAuthorizeUrl(String platform) {
        AppProperties.OAuthProvider provider = resolveProvider(platform);
        validateProvider(provider, platform);
        String state = UUID.randomUUID().toString().replace("-", "");
        String redirectUri = appProperties.getOauth().getRedirectBaseUrl() + "/api/profile/oauth/" + platform + "/callback";

        if ("wechat".equalsIgnoreCase(platform)) {
            return provider.getAuthorizeUrl()
                    + "?appid=" + encode(provider.getClientId())
                    + "&redirect_uri=" + encode(redirectUri)
                    + "&response_type=code"
                    + "&scope=" + encode(provider.getScope())
                    + "&state=" + encode(state)
                    + "#wechat_redirect";
        }

        return provider.getAuthorizeUrl()
                + "?app_id=" + encode(provider.getClientId())
                + "&redirect_uri=" + encode(redirectUri)
                + "&response_type=code"
                + "&scope=" + encode(provider.getScope())
                + "&state=" + encode(state);
    }

    public String handleCallback(String platform, String code) {
        AppProperties.OAuthProvider provider = resolveProvider(platform);
        validateProvider(provider, platform);
        Map<String, Object> tokenPayload = exchangeToken(platform, provider, code);
        Map<String, Object> userPayload = fetchUserInfo(platform, provider, tokenPayload);
        saveBinding(platform, tokenPayload, userPayload);
        return "/?oauth=success&platform=" + platform + "&message=" + encode(platform + " OAuth 绑定成功");
    }

    private Map<String, Object> exchangeToken(String platform, AppProperties.OAuthProvider provider, String code) {
        if ("wechat".equalsIgnoreCase(platform)) {
            return restClient.get()
                    .uri(provider.getTokenUrl()
                            + "?appid=" + encode(provider.getClientId())
                            + "&secret=" + encode(provider.getClientSecret())
                            + "&code=" + encode(code)
                            + "&grant_type=authorization_code")
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("grant_type", "authorization_code");
        body.put("code", code);
        body.put("app_id", provider.getClientId());
        body.put("app_secret", provider.getClientSecret());
        return restClient.post()
                .uri(provider.getTokenUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    private Map<String, Object> fetchUserInfo(String platform, AppProperties.OAuthProvider provider, Map<String, Object> tokenPayload) {
        if ("wechat".equalsIgnoreCase(platform)) {
            String accessToken = String.valueOf(tokenPayload.getOrDefault("access_token", ""));
            String openId = String.valueOf(tokenPayload.getOrDefault("openid", ""));
            return restClient.get()
                    .uri(provider.getUserInfoUrl()
                            + "?access_token=" + encode(accessToken)
                            + "&openid=" + encode(openId))
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        }

        String accessToken = extractFeishuToken(tokenPayload);
        return restClient.get()
                .uri(provider.getUserInfoUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    private String extractFeishuToken(Map<String, Object> tokenPayload) {
        Object data = tokenPayload.get("data");
        if (data instanceof Map<?, ?> map) {
            Object token = map.get("access_token");
            if (token != null) {
                return String.valueOf(token);
            }
        }
        Object token = tokenPayload.get("access_token");
        return token == null ? "" : String.valueOf(token);
    }

    private void saveBinding(String platform, Map<String, Object> tokenPayload, Map<String, Object> userPayload) {
        Long userId = currentUserService.requireCurrentUserId();
        UserSocialBinding binding = userSocialBindingRepository.findByUserIdOrderByPlatformAsc(userId)
                .stream()
                .filter(item -> platform.equalsIgnoreCase(item.getPlatform()))
                .findFirst()
                .orElseGet(UserSocialBinding::new);

        binding.setUserId(userId);
        binding.setPlatform(platform);
        binding.setOpenId(resolveOpenId(platform, tokenPayload, userPayload));
        binding.setMetaJson(writeJson(userPayload));
        if (binding.getBoundAt() == null) {
            binding.setBoundAt(LocalDateTime.now());
        }
        userSocialBindingRepository.save(binding);
    }

    private String resolveOpenId(String platform, Map<String, Object> tokenPayload, Map<String, Object> userPayload) {
        if ("wechat".equalsIgnoreCase(platform)) {
            return String.valueOf(tokenPayload.getOrDefault("openid", ""));
        }
        Object data = userPayload.get("data");
        if (data instanceof Map<?, ?> map) {
            Object openId = map.get("open_id");
            if (openId != null) {
                return String.valueOf(openId);
            }
            Object unionId = map.get("union_id");
            if (unionId != null) {
                return String.valueOf(unionId);
            }
        }
        return String.valueOf(userPayload.getOrDefault("open_id", ""));
    }

    private void validateProvider(AppProperties.OAuthProvider provider, String platform) {
        if (!provider.isEnabled()) {
            throw new IllegalStateException(platform + " OAuth 未启用");
        }
        if (!StringUtils.hasText(provider.getClientId()) || !StringUtils.hasText(provider.getClientSecret())) {
            throw new IllegalStateException(platform + " OAuth 配置不完整");
        }
    }

    private AppProperties.OAuthProvider resolveProvider(String platform) {
        return switch (platform.toLowerCase()) {
            case "wechat" -> appProperties.getOauth().getWechat();
            case "feishu" -> appProperties.getOauth().getFeishu();
            default -> throw new IllegalArgumentException("不支持的平台: " + platform);
        };
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }
}
