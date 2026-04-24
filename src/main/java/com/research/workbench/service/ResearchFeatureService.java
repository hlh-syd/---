package com.research.workbench.service;

import com.research.workbench.service.CurrentUserService;
import com.research.workbench.domain.KnowledgeBase;
import com.research.workbench.domain.PaperSearchResult;
import com.research.workbench.domain.PaperSearchTask;
import com.research.workbench.integration.LlmChatClient;
import com.research.workbench.service.KnowledgeBaseService;
import com.research.workbench.repository.KnowledgeBaseRepository;
import com.research.workbench.repository.PaperSearchResultRepository;
import com.research.workbench.repository.PaperSearchTaskRepository;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ResearchFeatureService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final PaperSearchTaskRepository paperSearchTaskRepository;
    private final PaperSearchResultRepository paperSearchResultRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeBaseService knowledgeBaseService;
    private final LlmChatClient llmChatClient;
    private final CurrentUserService currentUserService;

    public ResearchFeatureService(
            PaperSearchTaskRepository paperSearchTaskRepository,
            PaperSearchResultRepository paperSearchResultRepository,
            KnowledgeBaseRepository knowledgeBaseRepository,
            KnowledgeBaseService knowledgeBaseService,
            LlmChatClient llmChatClient,
            CurrentUserService currentUserService
    ) {
        this.paperSearchTaskRepository = paperSearchTaskRepository;
        this.paperSearchResultRepository = paperSearchResultRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.knowledgeBaseService = knowledgeBaseService;
        this.llmChatClient = llmChatClient;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> history() {
        return paperSearchTaskRepository.findTop10ByUserIdOrderByCreatedAtDesc(currentUserService.requireCurrentUserId())
                .stream()
                .map(task -> Map.<String, Object>of(
                        "id", task.getId(),
                        "query", task.getQueryText(),
                        "resultCount", task.getResultCount(),
                        "createdAt", task.getCreatedAt() == null ? "" : task.getCreatedAt().format(DATE_TIME_FORMATTER)
                ))
                .toList();
    }

    public Map<String, Object> summary(Long resultId) {
        PaperSearchResult result = requireResult(resultId);
        String answer = callLlmOrFallback(
                "请用中文快速总结这篇资料的核心价值、适用场景和下一步研究建议。",
                "标题：" + result.getTitle() + "\n摘要：" + nullToEmpty(result.getAbstractText())
        );
        return Map.of("resultId", resultId, "summary", answer);
    }

    public Map<String, Object> mindmap(Long resultId) {
        PaperSearchResult result = requireResult(resultId);
        String mermaid = callLlmOrFallback(
                "请把下面资料转成 Mermaid mindmap，输出纯 Mermaid 代码。",
                "标题：" + result.getTitle() + "\n摘要：" + nullToEmpty(result.getAbstractText())
        );
        if (!mermaid.contains("mindmap")) {
            mermaid = """
                    mindmap
                      root((%s))
                        背景
                          研究问题
                        方法
                          证据链
                          检索
                        输出
                          总结
                          入库
                    """.formatted(result.getTitle());
        }
        return Map.of("resultId", resultId, "mindmap", mermaid);
    }

    public Map<String, Object> saveToKnowledge(Long resultId, Long kbId) {
        PaperSearchResult result = requireResult(resultId);
        KnowledgeBase base = requireKnowledgeBase(kbId);
        Map<String, Object> saved = knowledgeBaseService.createTextDocument(
                base.getId(),
                result.getTitle(),
                "来源：" + nullToEmpty(result.getSourceName()) + "\n作者：" + nullToEmpty(result.getAuthors()) + "\n摘要：" + nullToEmpty(result.getAbstractText()),
                "PAPER"
        );
        return Map.of("saved", saved);
    }

    public Map<String, Object> saveTaskToKnowledge(Long taskId, Long kbId) {
        KnowledgeBase base = requireKnowledgeBase(kbId);
        PaperSearchTask task = requireTask(taskId);
        List<PaperSearchResult> results = paperSearchResultRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
        String content = results.stream()
                .map(item -> "标题：" + item.getTitle() + "\n来源：" + nullToEmpty(item.getSourceName()) + "\n摘要：" + nullToEmpty(item.getAbstractText()))
                .reduce("", (a, b) -> a + "\n\n" + b)
                .trim();
        Map<String, Object> saved = knowledgeBaseService.createTextDocument(base.getId(), "学术历史-" + task.getQueryText(), content, "PAPER_HISTORY");
        return Map.of("saved", saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> knowledgeBases() {
        return knowledgeBaseRepository.findByOwnerUserIdOrderByUpdatedAtDesc(currentUserService.requireCurrentUserId())
                .stream()
                .map(base -> Map.<String, Object>of("id", base.getId(), "name", base.getName()))
                .toList();
    }

    private PaperSearchResult requireResult(Long resultId) {
        PaperSearchResult result = paperSearchResultRepository.findById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("论文结果不存在: " + resultId));
        requireTask(result.getTaskId());
        return result;
    }

    private PaperSearchTask requireTask(Long taskId) {
        PaperSearchTask task = paperSearchTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("学术搜索历史不存在: " + taskId));
        if (!currentUserService.requireCurrentUserId().equals(task.getUserId())) {
            throw new IllegalArgumentException("无权访问该学术搜索记录");
        }
        return task;
    }

    private KnowledgeBase requireKnowledgeBase(Long kbId) {
        KnowledgeBase base = knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> new IllegalArgumentException("知识库不存在: " + kbId));
        if (!currentUserService.requireCurrentUserId().equals(base.getOwnerUserId())) {
            throw new IllegalArgumentException("无权访问该知识库");
        }
        return base;
    }

    private String callLlmOrFallback(String systemPrompt, String userPrompt) {
        try {
            return llmChatClient.chat(systemPrompt, userPrompt);
        } catch (Exception ignored) {
            return "快速总结：该资料强调研究问题驱动检索、证据回链和可沉淀输出，适合直接纳入知识库并生成后续计划。";
        }
    }

    private String nullToEmpty(String text) {
        return text == null ? "" : text;
    }
}
