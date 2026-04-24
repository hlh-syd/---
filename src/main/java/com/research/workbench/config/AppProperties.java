package com.research.workbench.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final FileStorage file = new FileStorage();
    private final AiAssistant aiAssistant = new AiAssistant();
    private final External external = new External();
    private final OAuth oauth = new OAuth();

    @Getter
    @Setter
    public static class FileStorage {
        private String uploadDir = "uploads";
    }

    @Getter
    @Setter
    public static class AiAssistant {
        private boolean enabled = true;
        private String llmProvider = "alibaba";
        private final Provider openai = new Provider();
        private final Provider alibaba = new Provider();
        private final Metaso metaso = new Metaso();
    }

    @Getter
    @Setter
    public static class Provider {
        private String apiKey;
        private String baseUrl;
        private String model;
        private Double temperature = 0.35;
        private Integer maxTokens = 2048;
    }

    @Getter
    @Setter
    public static class Metaso {
        private boolean enabled = true;
        private String apiKey;
        private String baseUrl = "https://metaso.cn/api/v1";
        private String searchPath = "/search";
        private String scope = "webpage";
        private Integer resultSize = 8;
        private Boolean concise = true;
    }

    @Getter
    @Setter
    public static class External {
        private boolean allowFallback = true;
    }

    @Getter
    @Setter
    public static class OAuth {
        private String redirectBaseUrl = "http://localhost:5000";
        private final OAuthProvider wechat = new OAuthProvider();
        private final OAuthProvider feishu = new OAuthProvider();
    }

    @Getter
    @Setter
    public static class OAuthProvider {
        private boolean enabled = false;
        private String clientId;
        private String clientSecret;
        private String authorizeUrl;
        private String tokenUrl;
        private String userInfoUrl;
        private String scope;
    }
}
