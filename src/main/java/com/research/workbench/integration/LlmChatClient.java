package com.research.workbench.integration;

import com.research.workbench.config.AppProperties;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class LlmChatClient {

    private final RestClient restClient;
    private final AppProperties appProperties;

    public LlmChatClient(RestClient.Builder restClientBuilder, AppProperties appProperties) {
        this.restClient = restClientBuilder.build();
        this.appProperties = appProperties;
    }

    public String chat(String systemPrompt, String userPrompt) {
        AppProperties.Provider provider = resolveProvider();
        if (!StringUtils.hasText(provider.getApiKey()) || !StringUtils.hasText(provider.getBaseUrl()) || !StringUtils.hasText(provider.getModel())) {
            throw new IllegalStateException("LLM 提供方配置不完整");
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", provider.getModel());
        requestBody.put("temperature", provider.getTemperature());
        requestBody.put("max_tokens", provider.getMaxTokens());
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));

        Map<String, Object> response = restClient.post()
                .uri(normalizeUrl(provider.getBaseUrl(), "/chat/completions"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + provider.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        if (response == null) {
            throw new IllegalStateException("LLM 返回为空");
        }
        Object choices = response.get("choices");
        if (choices instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            Object message = first.get("message");
            if (message instanceof Map<?, ?> messageMap) {
                Object content = messageMap.get("content");
                if (content != null) {
                    return String.valueOf(content);
                }
            }
        }
        throw new IllegalStateException("LLM 响应格式无法识别");
    }

    private AppProperties.Provider resolveProvider() {
        String providerCode = appProperties.getAiAssistant().getLlmProvider();
        if ("openai".equalsIgnoreCase(providerCode)) {
            return appProperties.getAiAssistant().getOpenai();
        }
        return appProperties.getAiAssistant().getAlibaba();
    }

    private String normalizeUrl(String baseUrl, String path) {
        String safeBase = Objects.requireNonNullElse(baseUrl, "").replaceAll("/+$", "");
        String safePath = Objects.requireNonNullElse(path, "");
        if (!safePath.startsWith("/")) {
            safePath = "/" + safePath;
        }
        return safeBase + safePath;
    }
}
