package com.research.workbench.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.research.workbench.service.CurrentUserService;
import com.research.workbench.config.AppProperties;
import com.research.workbench.domain.KnowledgeBase;
import com.research.workbench.domain.KnowledgeChunk;
import com.research.workbench.domain.KnowledgeDocument;
import com.research.workbench.integration.LlmChatClient;
import com.research.workbench.repository.KnowledgeBaseRepository;
import com.research.workbench.repository.KnowledgeChunkRepository;
import com.research.workbench.repository.KnowledgeDocumentRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class KnowledgeRagService {

    private static final int CHUNK_SIZE = 800;
    private static final int CHUNK_OVERLAP = 120;

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeExtractionService knowledgeExtractionService;
    private final LlmChatClient llmChatClient;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final CurrentUserService currentUserService;

    public KnowledgeRagService(
            KnowledgeBaseRepository knowledgeBaseRepository,
            KnowledgeDocumentRepository knowledgeDocumentRepository,
            KnowledgeChunkRepository knowledgeChunkRepository,
            KnowledgeExtractionService knowledgeExtractionService,
            LlmChatClient llmChatClient,
            AppProperties appProperties,
            ObjectMapper objectMapper,
            CurrentUserService currentUserService
    ) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.knowledgeExtractionService = knowledgeExtractionService;
        this.llmChatClient = llmChatClient;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.currentUserService = currentUserService;
    }

    public Map<String, Object> parseDocument(Long documentId) {
        KnowledgeDocument document = requireDocument(documentId);
        String extracted = knowledgeExtractionService.extractText(document.getStoragePath(), document.getFileExt(), document.getSummaryText());
        if (!StringUtils.hasText(extracted)) {
            document.setParseStatus("FAILED");
            knowledgeDocumentRepository.save(document);
            return Map.of(
                    "documentId", document.getId(),
                    "status", "FAILED",
                    "chunkCount", 0,
                    "message", "未提取到可解析文本"
            );
        }

        knowledgeChunkRepository.deleteByDocumentId(document.getId());
        List<String> chunks = splitIntoChunks(extracted);
        List<KnowledgeChunk> saved = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setDocumentId(document.getId());
            chunk.setChunkNo(i + 1);
            chunk.setContentText(chunks.get(i));
            chunk.setTokenCount(estimateTokens(chunks.get(i)));
            chunk.setKeywordJson(toKeywordJson(chunks.get(i)));
            saved.add(knowledgeChunkRepository.save(chunk));
        }

        document.setChunkCount(saved.size());
        document.setParseStatus("PARSED");
        document.setSummaryText(buildSummary(extracted));
        knowledgeDocumentRepository.save(document);
        refreshBaseStats(document.getKbId());

        return Map.of(
                "documentId", document.getId(),
                "status", document.getParseStatus(),
                "chunkCount", saved.size(),
                "summary", document.getSummaryText()
        );
    }

    public List<Map<String, Object>> parseBase(Long kbId) {
        requireBase(kbId);
        return knowledgeDocumentRepository.findByKbIdOrderByUpdatedAtDesc(kbId)
                .stream()
                .map(document -> parseDocument(document.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listCitations(Long documentId) {
        KnowledgeDocument document = requireDocument(documentId);
        return knowledgeChunkRepository.findByDocumentIdOrderByChunkNoAsc(documentId)
                .stream()
                .map(chunk -> Map.<String, Object>of(
                        "chunkId", chunk.getId(),
                        "documentId", documentId,
                        "documentTitle", document.getTitle(),
                        "chunkNo", chunk.getChunkNo(),
                        "content", chunk.getContentText(),
                        "tokenCount", chunk.getTokenCount()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> answerQuestion(Long kbId, String question) {
        return answerQuestion(kbId, question, List.of());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> answerQuestion(Long kbId, String question, List<String> history) {
        requireBase(kbId);
        String retrievalQuery = buildRetrievalQuery(question, history);
        List<ScoredChunk> topChunks = retrieveTopChunks(kbId, retrievalQuery);

        String answer = generateAnswer(question, topChunks);
        List<Map<String, Object>> citations = topChunks.stream()
                .map(item -> Map.<String, Object>of(
                        "documentId", item.document().getId(),
                        "documentTitle", item.document().getTitle(),
                        "chunkId", item.chunk().getId(),
                        "chunkNo", item.chunk().getChunkNo(),
                        "score", item.score(),
                        "excerpt", trim(item.chunk().getContentText(), 220)
                ))
                .toList();

        return new LinkedHashMap<>(Map.of(
                "question", question,
                "answer", answer,
                "citations", citations,
                "sourceRefsJson", writeJson(citations)
        ));
    }

    private String generateAnswer(String question, List<ScoredChunk> topChunks) {
        String evidence = topChunks.stream()
                .map(item -> "文档《" + item.document().getTitle() + "》片段 " + item.chunk().getChunkNo() + ":\n" + item.chunk().getContentText())
                .collect(Collectors.joining("\n\n"));

        if (StringUtils.hasText(evidence)) {
            try {
                return llmChatClient.chat(
                        """
                        你是知识库问答助手。
                        你只能基于给定证据回答，并在回答中明确指出结论来自哪些资料片段。
                        如果证据不足，要明确说明。
                        """,
                        """
                        用户问题：
                        %s

                        已检索证据：
                        %s

                        请给出中文回答，格式包含：
                        1. 结论
                        2. 证据依据
                        3. 后续建议
                        """.formatted(question, evidence)
                );
            } catch (Exception ex) {
                if (!appProperties.getExternal().isAllowFallback()) {
                    throw ex;
                }
            }
        }

        if (topChunks.isEmpty()) {
            return "当前知识库中没有找到足够证据来回答这个问题，建议先解析更多文档或补充资料。";
        }

        String summary = topChunks.stream()
                .map(item -> "《" + item.document().getTitle() + "》片段 " + item.chunk().getChunkNo() + " 提到：" + trim(item.chunk().getContentText(), 120))
                .collect(Collectors.joining("\n"));
        return """
                当前基于知识库检索到以下证据：
                %s

                初步结论：
                这些资料与问题高度相关，建议优先阅读引用片段并将结论沉淀为结构化笔记。
                """.formatted(summary);
    }

    private int scoreChunk(String question, String content) {
        if (!StringUtils.hasText(content)) {
            return 0;
        }
        Set<String> questionTerms = tokenize(question);
        Set<String> contentTerms = tokenize(content);
        int score = 0;
        for (String term : questionTerms) {
            if (contentTerms.contains(term)) {
                score += Math.max(1, term.length());
            }
        }
        return score;
    }

    private List<ScoredChunk> retrieveTopChunks(Long kbId, String retrievalQuery) {
        List<KnowledgeDocument> documents = knowledgeDocumentRepository.findByKbIdOrderByUpdatedAtDesc(kbId);
        List<ScoredChunk> ranked = new ArrayList<>();
        for (KnowledgeDocument document : documents) {
            for (KnowledgeChunk chunk : knowledgeChunkRepository.findByDocumentIdOrderByChunkNoAsc(document.getId())) {
                int score = scoreChunk(retrievalQuery, chunk.getContentText());
                if (score > 0 || ranked.size() < 5) {
                    ranked.add(new ScoredChunk(document, chunk, score));
                }
            }
        }
        return ranked.stream()
                .sorted(Comparator.comparingInt(ScoredChunk::score).reversed())
                .limit(5)
                .toList();
    }

    private String buildRetrievalQuery(String question, List<String> history) {
        StringBuilder builder = new StringBuilder();
        if (history != null) {
            history.stream()
                    .filter(StringUtils::hasText)
                    .skip(Math.max(0, history.size() - 4))
                    .forEach(item -> builder.append(item).append('\n'));
        }
        builder.append(question);
        return builder.toString();
    }

    private Set<String> tokenize(String text) {
        if (!StringUtils.hasText(text)) {
            return Set.of();
        }
        return List.of(text.toLowerCase(Locale.ROOT)
                        .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\u4e00-\\u9fa5]+", " ")
                        .trim()
                        .split("\\s+"))
                .stream()
                .filter(token -> !token.isBlank() && token.length() > 1)
                .collect(Collectors.toSet());
    }

    private List<String> splitIntoChunks(String text) {
        String normalized = text.replace("\r", "").trim();
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(normalized.length(), start + CHUNK_SIZE);
            chunks.add(normalized.substring(start, end).trim());
            if (end == normalized.length()) {
                break;
            }
            start = Math.max(start + 1, end - CHUNK_OVERLAP);
        }
        return chunks;
    }

    private int estimateTokens(String text) {
        return Math.max(1, text.length() / 4);
    }

    private String buildSummary(String text) {
        return trim(text.replaceAll("\\s+", " "), 260);
    }

    private String trim(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private String toKeywordJson(String text) {
        Set<String> keywords = tokenize(text).stream().limit(8).collect(Collectors.toSet());
        return keywords.stream().map(item -> "\"" + item + "\"").collect(Collectors.joining(",", "[", "]"));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化引用失败", e);
        }
    }

    private void refreshBaseStats(Long kbId) {
        KnowledgeBase base = requireBase(kbId);
        List<KnowledgeDocument> documents = knowledgeDocumentRepository.findByKbIdOrderByUpdatedAtDesc(kbId);
        base.setDocCount(documents.size());
        base.setTotalChunkCount(documents.stream().mapToInt(document -> document.getChunkCount() == null ? 0 : document.getChunkCount()).sum());
        knowledgeBaseRepository.save(base);
    }

    private KnowledgeDocument requireDocument(Long documentId) {
        KnowledgeDocument document = knowledgeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在: " + documentId));
        requireBase(document.getKbId());
        return document;
    }

    private KnowledgeBase requireBase(Long kbId) {
        KnowledgeBase base = knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> new IllegalArgumentException("知识库不存在: " + kbId));
        if (!currentUserService.requireCurrentUserId().equals(base.getOwnerUserId())) {
            throw new IllegalArgumentException("无权访问该知识库");
        }
        return base;
    }

    private record ScoredChunk(KnowledgeDocument document, KnowledgeChunk chunk, int score) {
    }
}
