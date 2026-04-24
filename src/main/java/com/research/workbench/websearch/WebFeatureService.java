package com.research.workbench.websearch;

import com.research.workbench.auth.CurrentUserService;
import com.research.workbench.domain.KnowledgeBase;
import com.research.workbench.domain.WebSearchResult;
import com.research.workbench.domain.WebSearchTask;
import com.research.workbench.knowledge.KnowledgeBaseService;
import com.research.workbench.repository.KnowledgeBaseRepository;
import com.research.workbench.repository.WebSearchResultRepository;
import com.research.workbench.repository.WebSearchTaskRepository;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WebFeatureService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final WebSearchTaskRepository webSearchTaskRepository;
    private final WebSearchResultRepository webSearchResultRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeBaseService knowledgeBaseService;
    private final CurrentUserService currentUserService;

    public WebFeatureService(
            WebSearchTaskRepository webSearchTaskRepository,
            WebSearchResultRepository webSearchResultRepository,
            KnowledgeBaseRepository knowledgeBaseRepository,
            KnowledgeBaseService knowledgeBaseService,
            CurrentUserService currentUserService
    ) {
        this.webSearchTaskRepository = webSearchTaskRepository;
        this.webSearchResultRepository = webSearchResultRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.knowledgeBaseService = knowledgeBaseService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> history() {
        return webSearchTaskRepository.findTop10ByUserIdOrderByUpdatedAtDesc(currentUserService.requireCurrentUserId())
                .stream()
                .map(task -> Map.<String, Object>of(
                        "id", task.getId(),
                        "query", task.getQueryText(),
                        "resultCount", task.getResultCount(),
                        "createdAt", task.getUpdatedAt() == null ? "" : task.getUpdatedAt().format(DATE_TIME_FORMATTER),
                        "markdown", task.getMarkdownSummary() == null ? "" : task.getMarkdownSummary()
                ))
                .toList();
    }

    public Map<String, Object> save(Long resultId, Long kbId) {
        WebSearchResult result = requireResult(resultId);
        KnowledgeBase base = requireKnowledgeBase(kbId);
        Map<String, Object> saved = knowledgeBaseService.createTextDocument(
                base.getId(),
                result.getTitle(),
                "平台：" + nullToEmpty(result.getPlatformName()) + "\n作者：" + nullToEmpty(result.getAuthorName()) + "\n内容：" + nullToEmpty(result.getSnippetText()),
                "WEB"
        );
        return Map.of("saved", saved);
    }

    public Map<String, Object> saveTask(Long taskId, Long kbId) {
        KnowledgeBase base = requireKnowledgeBase(kbId);
        WebSearchTask task = requireTask(taskId);
        String content = (task.getMarkdownSummary() == null ? "" : task.getMarkdownSummary()).trim();
        if (content.isBlank()) {
            content = webSearchResultRepository.findByTaskIdOrderByCreatedAtAsc(taskId)
                    .stream()
                    .map(item -> item.getTitle() + "：" + nullToEmpty(item.getSnippetText()))
                    .reduce("", (a, b) -> a + "\n" + b)
                    .trim();
        }
        Map<String, Object> saved = knowledgeBaseService.createTextDocument(base.getId(), "网页历史-" + task.getQueryText(), content, "WEB_HISTORY");
        return Map.of("saved", saved);
    }

    private WebSearchResult requireResult(Long resultId) {
        WebSearchResult result = webSearchResultRepository.findById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("网页结果不存在: " + resultId));
        requireTask(result.getTaskId());
        return result;
    }

    private WebSearchTask requireTask(Long taskId) {
        WebSearchTask task = webSearchTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("网页搜索历史不存在: " + taskId));
        if (!currentUserService.requireCurrentUserId().equals(task.getUserId())) {
            throw new IllegalArgumentException("无权访问该网页搜索记录");
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

    private String nullToEmpty(String text) {
        return text == null ? "" : text;
    }
}
