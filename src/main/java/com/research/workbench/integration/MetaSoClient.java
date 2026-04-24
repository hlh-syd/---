package com.research.workbench.integration;

import com.research.workbench.config.AppProperties;
import java.util.ArrayList;
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
public class MetaSoClient {

    private final RestClient restClient;
    private final AppProperties appProperties;
    public MetaSoClient(RestClient.Builder restClientBuilder, AppProperties appProperties) {
        this.restClient = restClientBuilder.build();
        this.appProperties = appProperties;
    }

    public List<Map<String, Object>> search(String query, String scope, Integer size) {
        AppProperties.Metaso metaso = appProperties.getAiAssistant().getMetaso();
        if (!metaso.isEnabled() || !StringUtils.hasText(metaso.getApiKey())) {
            throw new IllegalStateException("MetaSo API 未配置");
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("q", query);
        requestBody.put("scope", StringUtils.hasText(scope) ? scope : metaso.getScope());
        requestBody.put("size", size == null ? metaso.getResultSize() : size);
        requestBody.put("page", 1);
        requestBody.put("concise", metaso.getConcise());

        Map<String, Object> response = restClient.post()
                .uri(normalizeUrl(metaso.getBaseUrl(), metaso.getSearchPath()))
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + metaso.getApiKey())
                .header("X-API-Key", metaso.getApiKey())
                .body(requestBody)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        return flattenResults(response);
    }

    private List<Map<String, Object>> flattenResults(Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            return List.of();
        }
        Object candidate = firstNonNull(
                response.get("results"),
                response.get("items"),
                response.get("data"),
                response.get("list")
        );
        return normalizeList(candidate);
    }

    private List<Map<String, Object>> normalizeList(Object candidate) {
        if (candidate == null) {
            return List.of();
        }
        List<Map<String, Object>> normalized = new ArrayList<>();
        if (candidate instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    normalized.add(normalizeItem(map));
                }
            }
            return normalized;
        }
        if (candidate instanceof Map<?, ?> wrapper) {
            for (Object nested : wrapper.values()) {
                normalized.addAll(normalizeList(nested));
            }
        }
        return normalized;
    }

    private Map<String, Object> normalizeItem(Map<?, ?> map) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("title", firstText(map, "title", "name", "heading"));
        item.put("url", firstText(map, "url", "link", "sourceUrl"));
        item.put("snippet", firstText(map, "snippet", "summary", "content", "desc", "description"));
        item.put("source", firstText(map, "source", "site", "platform", "domain"));
        item.put("author", firstText(map, "author", "authorName", "creator"));
        return item;
    }

    private String firstText(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value);
            }
        }
        return "";
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
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
