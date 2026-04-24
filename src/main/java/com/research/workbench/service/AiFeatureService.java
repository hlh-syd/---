package com.research.workbench.service;

import com.research.workbench.integration.LlmChatClient;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AiFeatureService {

    private final LlmChatClient llmChatClient;

    public AiFeatureService(LlmChatClient llmChatClient) {
        this.llmChatClient = llmChatClient;
    }

    public List<Map<String, Object>> toolList() {
        return List.of(
                Map.of("name", "Perplexity", "url", "https://www.perplexity.ai/", "category", "Research Search"),
                Map.of("name", "NotebookLM", "url", "https://notebooklm.google.com/", "category", "Knowledge Synthesis"),
                Map.of("name", "Elicit", "url", "https://elicit.com/", "category", "Literature Review"),
                Map.of("name", "ResearchRabbit", "url", "https://www.researchrabbit.ai/", "category", "Paper Graph")
        );
    }

    public Map<String, Object> optimizePrompt(String prompt) {
        String answer = callOrFallback(
                "你是提示词优化器，请把用户输入改写成结构化、高可执行的中文提示词。",
                prompt,
                "优化后的提示词：\n目标、输入、输出格式、限制条件、评价标准。"
        );
        return Map.of("result", answer);
    }

    public Map<String, Object> bioAssistant(String prompt) {
        String answer = callOrFallback(
                "你是 Bio 生物医学助手，请用中文回答，强调实验设计、数据解释和风险提示。",
                prompt,
                "Bio 助手建议：请先明确样本来源、实验条件、统计方法和潜在偏差，再推进后续实验或分析。"
        );
        return Map.of("result", answer);
    }

    private String callOrFallback(String systemPrompt, String userPrompt, String fallback) {
        try {
            return llmChatClient.chat(systemPrompt, userPrompt);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
