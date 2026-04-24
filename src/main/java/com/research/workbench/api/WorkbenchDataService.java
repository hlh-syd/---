package com.research.workbench.api;

import com.research.workbench.auth.CurrentUserService;
import com.research.workbench.bootstrap.UserInitializationService;
import com.research.workbench.domain.AiChatMessage;
import com.research.workbench.domain.AiChatSession;
import com.research.workbench.domain.CalendarEvent;
import com.research.workbench.domain.CheckinRecord;
import com.research.workbench.domain.KnowledgeBase;
import com.research.workbench.domain.KnowledgeDocument;
import com.research.workbench.domain.PaperSearchResult;
import com.research.workbench.domain.PaperSearchTask;
import com.research.workbench.domain.PomodoroRecord;
import com.research.workbench.domain.ResearchProject;
import com.research.workbench.domain.StudyTask;
import com.research.workbench.domain.SysUser;
import com.research.workbench.domain.UserDailyUsage;
import com.research.workbench.domain.UserProfile;
import com.research.workbench.domain.UserSocialBinding;
import com.research.workbench.domain.WebSearchResult;
import com.research.workbench.domain.WebSearchTask;
import com.research.workbench.domain.Workspace;
import com.research.workbench.history.SearchHistoryService;
import com.research.workbench.integration.LlmChatClient;
import com.research.workbench.integration.MetaSoClient;
import com.research.workbench.repository.AiChatMessageRepository;
import com.research.workbench.repository.AiChatSessionRepository;
import com.research.workbench.repository.CalendarEventRepository;
import com.research.workbench.repository.CheckinRecordRepository;
import com.research.workbench.repository.KnowledgeBaseRepository;
import com.research.workbench.repository.KnowledgeDocumentRepository;
import com.research.workbench.repository.PaperSearchResultRepository;
import com.research.workbench.repository.PaperSearchTaskRepository;
import com.research.workbench.repository.PomodoroRecordRepository;
import com.research.workbench.repository.ResearchProjectRepository;
import com.research.workbench.repository.StudyTaskRepository;
import com.research.workbench.repository.SysUserRepository;
import com.research.workbench.repository.UserDailyUsageRepository;
import com.research.workbench.repository.UserProfileRepository;
import com.research.workbench.repository.UserSocialBindingRepository;
import com.research.workbench.repository.WebSearchResultRepository;
import com.research.workbench.repository.WebSearchTaskRepository;
import com.research.workbench.repository.WorkspaceArtifactRepository;
import com.research.workbench.repository.WorkspaceRepository;
import com.research.workbench.config.AppProperties;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WorkbenchDataService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final SysUserRepository sysUserRepository;
    private final UserDailyUsageRepository userDailyUsageRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserSocialBindingRepository userSocialBindingRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ResearchProjectRepository researchProjectRepository;
    private final PaperSearchTaskRepository paperSearchTaskRepository;
    private final PaperSearchResultRepository paperSearchResultRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final WebSearchTaskRepository webSearchTaskRepository;
    private final WebSearchResultRepository webSearchResultRepository;
    private final AiChatSessionRepository aiChatSessionRepository;
    private final AiChatMessageRepository aiChatMessageRepository;
    private final WorkspaceArtifactRepository workspaceArtifactRepository;
    private final StudyTaskRepository studyTaskRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final PomodoroRecordRepository pomodoroRecordRepository;
    private final CheckinRecordRepository checkinRecordRepository;
    private final MetaSoClient metaSoClient;
    private final LlmChatClient llmChatClient;
    private final AppProperties appProperties;
    private final CurrentUserService currentUserService;
    private final UserInitializationService userInitializationService;
    private final SearchHistoryService searchHistoryService;

    public WorkbenchDataService(
            SysUserRepository sysUserRepository,
            UserDailyUsageRepository userDailyUsageRepository,
            UserProfileRepository userProfileRepository,
            UserSocialBindingRepository userSocialBindingRepository,
            WorkspaceRepository workspaceRepository,
            ResearchProjectRepository researchProjectRepository,
            PaperSearchTaskRepository paperSearchTaskRepository,
            PaperSearchResultRepository paperSearchResultRepository,
            KnowledgeBaseRepository knowledgeBaseRepository,
            KnowledgeDocumentRepository knowledgeDocumentRepository,
            WebSearchTaskRepository webSearchTaskRepository,
            WebSearchResultRepository webSearchResultRepository,
            AiChatSessionRepository aiChatSessionRepository,
            AiChatMessageRepository aiChatMessageRepository,
            WorkspaceArtifactRepository workspaceArtifactRepository,
            StudyTaskRepository studyTaskRepository,
            CalendarEventRepository calendarEventRepository,
            PomodoroRecordRepository pomodoroRecordRepository,
            CheckinRecordRepository checkinRecordRepository,
            MetaSoClient metaSoClient,
            LlmChatClient llmChatClient,
            AppProperties appProperties,
            CurrentUserService currentUserService,
            UserInitializationService userInitializationService,
            SearchHistoryService searchHistoryService
    ) {
        this.sysUserRepository = sysUserRepository;
        this.userDailyUsageRepository = userDailyUsageRepository;
        this.userProfileRepository = userProfileRepository;
        this.userSocialBindingRepository = userSocialBindingRepository;
        this.workspaceRepository = workspaceRepository;
        this.researchProjectRepository = researchProjectRepository;
        this.paperSearchTaskRepository = paperSearchTaskRepository;
        this.paperSearchResultRepository = paperSearchResultRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.webSearchTaskRepository = webSearchTaskRepository;
        this.webSearchResultRepository = webSearchResultRepository;
        this.aiChatSessionRepository = aiChatSessionRepository;
        this.aiChatMessageRepository = aiChatMessageRepository;
        this.workspaceArtifactRepository = workspaceArtifactRepository;
        this.studyTaskRepository = studyTaskRepository;
        this.calendarEventRepository = calendarEventRepository;
        this.pomodoroRecordRepository = pomodoroRecordRepository;
        this.checkinRecordRepository = checkinRecordRepository;
        this.metaSoClient = metaSoClient;
        this.llmChatClient = llmChatClient;
        this.appProperties = appProperties;
        this.currentUserService = currentUserService;
        this.userInitializationService = userInitializationService;
        this.searchHistoryService = searchHistoryService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> bootstrap() {
        SysUser user = currentUserService.requireCurrentUser();
        userInitializationService.ensureInitialized(user);
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElseThrow();
        UserDailyUsage todayUsage = userDailyUsageRepository.findByUserIdAndUsageDate(user.getId(), LocalDate.now()).orElse(null);
        Workspace workspace = workspaceRepository.findByOwnerUserIdOrderByUpdatedAtDesc(user.getId()).stream().findFirst().orElseThrow();
        List<ResearchProject> projects = researchProjectRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspace.getId());
        List<PaperSearchTask> recentPaperTasks = paperSearchTaskRepository.findTop10ByUserIdOrderByCreatedAtDesc(user.getId());
        List<PaperSearchResult> recentPapers = loadPaperResults(recentPaperTasks);
        List<KnowledgeBase> knowledgeBases = knowledgeBaseRepository.findByOwnerUserIdOrderByUpdatedAtDesc(user.getId());
        Long firstKbId = knowledgeBases.isEmpty() ? null : knowledgeBases.get(0).getId();
        List<KnowledgeDocument> documents = firstKbId == null
                ? List.of()
                : knowledgeDocumentRepository.findTop20ByKbIdOrderByUpdatedAtDesc(firstKbId);
        List<WebSearchTask> webTasks = webSearchTaskRepository.findTop10ByUserIdOrderByUpdatedAtDesc(user.getId());
        List<WebSearchResult> webResults = loadWebResults(webTasks);
        AiChatSession session = aiChatSessionRepository.findTopByUserIdOrderByUpdatedAtDesc(user.getId()).orElseThrow();
        List<AiChatMessage> messages = aiChatMessageRepository.findTop20BySessionIdOrderByCreatedAtAsc(session.getId());
        List<StudyTask> tasks = studyTaskRepository.findTop20ByUserIdOrderByDueTimeAsc(user.getId());
        List<CalendarEvent> calendarEvents =
                calendarEventRepository.findTop10ByUserIdAndStartTimeGreaterThanEqualOrderByStartTimeAsc(user.getId(), LocalDate.now().atStartOfDay());
        List<PomodoroRecord> pomodoros = pomodoroRecordRepository.findTop20ByUserIdOrderByStartedAtDesc(user.getId());
        List<CheckinRecord> checkins = checkinRecordRepository.findTop20ByUserIdOrderByCheckinDateDesc(user.getId());
        List<UserSocialBinding> bindings = userSocialBindingRepository.findByUserIdOrderByPlatformAsc(user.getId());

        return linkedMap(
                "brand", linkedMap(
                        "name", workspace.getName(),
                        "tagline", "Research-first workspace for discovery, knowledge capture, AI generation and focused execution."
                ),
                "modules", List.of(
                        module("overview", "首页工作台", "今天最值得推进的研究动作"),
                        module("research", "学术调研", "围绕研究问题完成检索与筛选"),
                        module("knowledge", "知识库", "资料归档、引用回溯、问答再利用"),
                        module("web", "网页搜索", "抓取网页趋势并沉淀 Markdown"),
                        module("assistant", "AI 服务", "模板化输出与来源驱动问答"),
                        module("plan", "研究计划", "番茄钟、日历与推进管理"),
                        module("relax", "放松一下", "轻量娱乐功能，调节节奏"),
                        module("profile", "个人中心", "个人画像与研究资产展示")
                ),
                "dashboard", linkedMap(
                        "headline", linkedMap(
                                "title", workspace.getDescription(),
                                "summary", "把检索、收藏、知识库、AI 产出和研究计划拉到同一条主链路上。",
                                "chips", List.of("MetaSo 检索", "知识库问答", "引用回溯", "番茄钟推进")
                        ),
                        "metrics", buildDashboardMetrics(user.getId(), knowledgeBases, pomodoros),
                        "recentSearches", recentPaperTasks.stream().limit(3).map(this::toRecentSearchItem).toList(),
                        "focusTasks", tasks.stream().limit(3).map(this::toTaskCard).toList(),
                        "projects", projects.stream().limit(3).map(this::toProjectCard).toList(),
                        "activity", buildActivity(recentPaperTasks, webTasks, messages)
                ),
                "research", linkedMap(
                        "featuredQuery", recentPaperTasks.isEmpty() ? "请输入研究问题" : recentPaperTasks.get(0).getQueryText(),
                        "filters", linkedMap(
                                "years", List.of("2026", "2025", "2024", "2023"),
                                "types", List.of("方法学", "系统综述", "工作流", "工具研究"),
                                "methods", List.of("多组学", "RAG", "可视化", "质性研究")
                        ),
                        "results", recentPapers.stream().limit(6).map(this::toPaperCard).toList(),
                        "insights", buildResearchInsights(recentPapers)
                ),
                "knowledge", linkedMap(
                        "libraries", knowledgeBases.stream().map(this::toKnowledgeBaseCard).toList(),
                        "documents", documents.stream().map(this::toKnowledgeDocumentCard).toList(),
                        "references", documents.stream()
                                .map(KnowledgeDocument::getSummaryText)
                                .filter(text -> text != null && !text.isBlank())
                                .limit(3)
                                .toList()
                ),
                "webSearch", linkedMap(
                        "tabs", List.of("全部", "网页", "小红书", "抖音", "医学站点", "工具导航"),
                        "results", webResults.stream().limit(6).map(this::toWebCard).toList(),
                        "markdown", webTasks.isEmpty() ? "# 网页搜索简报\n\n暂无数据" : webTasks.get(0).getMarkdownSummary()
                ),
                "assistant", linkedMap(
                        "assistants", List.of(
                                assistant("提示词优化器", "把模糊需求改写成结构化提示词"),
                                assistant("Bio 医学助手", "支持疾病、实验与生信场景问答"),
                                assistant("知识库问答", "基于已入库资料进行引用式回答"),
                                assistant("文献简报", "自动生成 brief、研究笔记与后续任务")
                        ),
                        "messages", messages.stream().map(this::toMessageCard).toList(),
                        "templates", List.of("Systematic Review Brief", "Bio Marker Comparison", "竞品功能拆解", "知识库问答追问模板"),
                        "sources", parseJsonArray(session.getSourceDocIds())
                ),
                "plan", linkedMap(
                        "summary", buildPlanMetrics(user.getId(), checkins, tasks, pomodoros),
                        "tasks", tasks.stream().limit(6).map(this::toTaskCard).toList(),
                        "calendar", calendarEvents.stream().map(this::toCalendarCard).toList(),
                        "focusTrend", buildFocusTrend(user.getId())
                ),
                "profile", linkedMap(
                        "user", linkedMap(
                                "name", firstNonBlank(profile.getRealName(), user.getNickname()),
                                "role", composeRole(profile.getDegreeLevel(), profile.getResearchDirection()),
                                "bio", nullToEmpty(profile.getBio()),
                                "institution", nullToEmpty(profile.getInstitution()),
                                "gender", normalizeGender(profile.getGender()),
                                "avatarUrl", nullToEmpty(user.getAvatarUrl())
                        ),
                        "detail", linkedMap(
                                "realName", nullToEmpty(profile.getRealName()),
                                "bio", nullToEmpty(profile.getBio()),
                                "institution", nullToEmpty(profile.getInstitution()),
                                "department", nullToEmpty(profile.getDepartment()),
                                "researchDirection", nullToEmpty(profile.getResearchDirection()),
                                "degreeLevel", nullToEmpty(profile.getDegreeLevel()),
                                "gender", normalizeGender(profile.getGender()),
                                "avatarUrl", nullToEmpty(user.getAvatarUrl())
                        ),
                        "bindings", bindings.stream().map(this::toBindingLabel).toList(),
                        "outputs", List.of(
                                "文献简报 " + workspaceArtifactRepository.countByCreatorUserId(user.getId()) + " 份",
                                "Markdown 总结 " + webTasks.size() + " 份",
                                "知识卡片 " + documents.size() + " 条",
                                "阶段报告 " + Math.min(workspaceArtifactRepository.countByCreatorUserId(user.getId()), 4) + " 份"
                        ),
                        "tags", parseJsonArray(profile.getTags()),
                        "todayUsageMinutes", todayUsage == null || todayUsage.getTodayTime() == null ? 0 : todayUsage.getTodayTime(),
                        "todayUsageText", formatTodayUsage(todayUsage == null || todayUsage.getTodayTime() == null ? 0 : todayUsage.getTodayTime())
                )
        );
    }

    public Map<String, Object> searchResearch(String query) {
        SysUser user = currentUserService.requireCurrentUser();
        userInitializationService.ensureInitialized(user);
        Workspace workspace = workspaceRepository.findByOwnerUserIdOrderByUpdatedAtDesc(user.getId()).stream().findFirst().orElseThrow();
        ResearchProject project = researchProjectRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspace.getId()).stream().findFirst().orElse(null);
        PaperSearchTask task = new PaperSearchTask();
        task.setUserId(user.getId());
        task.setWorkspaceId(workspace.getId());
        task.setProjectId(project == null ? null : project.getId());
        task.setQueryText(query == null || query.isBlank() ? "默认研究问题" : query.trim());
        task.setStartedAt(LocalDateTime.now());
        task = paperSearchTaskRepository.save(task);

        List<PaperSearchResult> matches = searchResearchResults(task, query);
        task.setResultCount(matches.size());
        task.setFinishedAt(LocalDateTime.now());
        paperSearchTaskRepository.save(task);

        Map<String, Object> response = linkedMap(
                "query", task.getQueryText(),
                "results", matches.stream().map(this::toPaperCard).toList(),
                "insights", List.of(
                        insight("命中结果", "共返回 " + matches.size() + " 条可参考条目。"),
                        insight("聚焦建议", "优先查看方法学和工作流类资料，方便形成可实施方案。"),
                        insight("下一步", "建议将高价值结果直接加入知识库并生成 brief。")
                )
        );
        searchHistoryService.appendDetail(
                "RESEARCH-" + task.getId(),
                user.getId(),
                user.getNickname(),
                "RESEARCH",
                task.getQueryText(),
                task.getQueryText(),
                response,
                Map.of("taskId", task.getId(), "resultCount", matches.size())
        );
        return response;
    }

    public Map<String, Object> searchWeb(String query, String platform) {
        SysUser user = currentUserService.requireCurrentUser();
        userInitializationService.ensureInitialized(user);
        Workspace workspace = workspaceRepository.findByOwnerUserIdOrderByUpdatedAtDesc(user.getId()).stream().findFirst().orElseThrow();
        ResearchProject project = researchProjectRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspace.getId()).stream().findFirst().orElse(null);
        WebSearchTask task = new WebSearchTask();
        task.setUserId(user.getId());
        task.setWorkspaceId(workspace.getId());
        task.setProjectId(project == null ? null : project.getId());
        task.setQueryText(query == null || query.isBlank() ? "默认主题" : query.trim());
        task.setPlatformScope(platform == null || platform.isBlank() ? "全部" : platform);
        task = webSearchTaskRepository.save(task);

        List<WebSearchResult> matches = searchWebResults(task, query, platform);
        String markdown = buildWebMarkdown(task.getQueryText(), matches, task.getPlatformScope());
        task.setResultCount(matches.size());
        task.setMarkdownSummary(markdown);
        webSearchTaskRepository.save(task);

        Map<String, Object> response = linkedMap(
                "query", task.getQueryText(),
                "results", matches.stream().map(this::toWebCard).toList(),
                "markdown", markdown
        );
        searchHistoryService.appendDetail(
                "WEB-" + task.getId(),
                user.getId(),
                user.getNickname(),
                "WEB",
                task.getQueryText(),
                linkedMap("query", task.getQueryText(), "platform", task.getPlatformScope()),
                response,
                Map.of("taskId", task.getId(), "resultCount", matches.size())
        );
        return response;
    }

    public Map<String, Object> replyAssistant(String prompt, List<String> sources) {
        SysUser user = currentUserService.requireCurrentUser();
        userInitializationService.ensureInitialized(user);
        Workspace workspace = workspaceRepository.findByOwnerUserIdOrderByUpdatedAtDesc(user.getId()).stream().findFirst().orElseThrow();
        ResearchProject project = researchProjectRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspace.getId()).stream().findFirst().orElse(null);
        String safePrompt = prompt == null || prompt.isBlank() ? "请生成本周研究推进建议" : prompt.trim();
        List<String> safeSources = (sources == null || sources.isEmpty())
                ? List.of("竞品调研.md", "多组学综述.pdf")
                : sources;

        AiChatSession session = aiChatSessionRepository.findTopByUserIdOrderByUpdatedAtDesc(user.getId())
                .orElseGet(() -> {
                    AiChatSession created = new AiChatSession();
                    created.setUserId(user.getId());
                    created.setWorkspaceId(workspace.getId());
                    created.setProjectId(project == null ? null : project.getId());
                    created.setTitle("默认助手会话");
                    created.setAssistantType("GENERAL");
                    created.setSourceDocIds(toJsonArray(safeSources));
                    return aiChatSessionRepository.save(created);
                });

        session.setSourceDocIds(toJsonArray(safeSources));
        aiChatSessionRepository.save(session);

        AiChatMessage userMessage = new AiChatMessage();
        userMessage.setSessionId(session.getId());
        userMessage.setRoleCode("USER");
        userMessage.setContentText(safePrompt);
        userMessage.setSourceRefs(toJsonArray(safeSources));
        aiChatMessageRepository.save(userMessage);

        String answer = generateAssistantReply(safePrompt, safeSources);

        AiChatMessage assistantMessage = new AiChatMessage();
        assistantMessage.setSessionId(session.getId());
        assistantMessage.setRoleCode("ASSISTANT");
        assistantMessage.setContentText(answer);
        assistantMessage.setModelName("qwen-plus");
        assistantMessage.setSourceRefs(toJsonArray(safeSources));
        aiChatMessageRepository.save(assistantMessage);

        Map<String, Object> response = linkedMap(
                "prompt", safePrompt,
                "answer", answer,
                "sourceRefs", safeSources,
                "createdAt", LocalDate.now().toString()
        );
        searchHistoryService.appendDetail(
                "GENERAL-" + session.getId(),
                user.getId(),
                user.getNickname(),
                "GENERAL_CHAT",
                session.getTitle(),
                safePrompt,
                answer,
                linkedMap("sources", safeSources, "aiSessionId", session.getId())
        );
        return response;
    }

    private List<PaperSearchResult> searchResearchResults(PaperSearchTask task, String query) {
        try {
            List<Map<String, Object>> external = metaSoClient.search(task.getQueryText(), "webpage", 8);
            if (external.isEmpty()) {
                return queryResearchFallbackForUser(query);
            }
            List<PaperSearchResult> saved = external.stream()
                    .map(item -> savePaperSearchResult(task.getId(), item))
                    .toList();
            return saved;
        } catch (Exception ex) {
            if (!appProperties.getExternal().isAllowFallback()) {
                throw ex;
            }
            return queryResearchFallbackForUser(query);
        }
    }

    private List<WebSearchResult> searchWebResults(WebSearchTask task, String query, String platform) {
        try {
            String enrichedQuery = enrichWebQuery(task.getQueryText(), platform);
            List<Map<String, Object>> external = metaSoClient.search(enrichedQuery, "webpage", 8);
            if (external.isEmpty()) {
                return queryWebFallbackForUser(query, platform);
            }
            return external.stream()
                    .map(item -> saveWebSearchResult(task.getId(), item))
                    .filter(item -> matchPlatform(item.getPlatformName(), platform))
                    .toList();
        } catch (Exception ex) {
            if (!appProperties.getExternal().isAllowFallback()) {
                throw ex;
            }
            return queryWebFallbackForUser(query, platform);
        }
    }

    private List<PaperSearchResult> queryResearchFallback(String query) {
        String normalized = normalize(query);
        return normalized.isBlank()
                ? paperSearchResultRepository.findTop20ByOrderByCreatedAtDesc()
                : paperSearchResultRepository
                .findDistinctByTitleContainingIgnoreCaseOrAbstractTextContainingIgnoreCaseOrderByPublishYearDesc(normalized, normalized);
    }

    private List<WebSearchResult> queryWebFallback(String query, String platform) {
        String normalized = normalize(query);
        List<WebSearchResult> results = normalized.isBlank()
                ? webSearchResultRepository.findTop20ByOrderByCreatedAtDesc()
                : webSearchResultRepository
                .findDistinctByTitleContainingIgnoreCaseOrSnippetTextContainingIgnoreCaseOrderByCreatedAtDesc(normalized, normalized);
        if (platform == null || platform.isBlank() || "全部".equals(platform)) {
            return results;
        }
        return results.stream().filter(item -> matchPlatform(item.getPlatformName(), platform)).toList();
    }

    private List<PaperSearchResult> queryResearchFallbackForUser(String query) {
        String normalized = normalize(query);
        List<PaperSearchTask> tasks = paperSearchTaskRepository.findTop10ByUserIdOrderByCreatedAtDesc(currentUserService.requireCurrentUserId());
        List<PaperSearchResult> results = loadPaperResults(tasks);
        if (normalized.isBlank()) {
            return results;
        }
        return results.stream()
                .filter(item -> containsIgnoreCase(item.getTitle(), normalized) || containsIgnoreCase(item.getAbstractText(), normalized))
                .toList();
    }

    private List<WebSearchResult> queryWebFallbackForUser(String query, String platform) {
        String normalized = normalize(query);
        List<WebSearchTask> tasks = webSearchTaskRepository.findTop10ByUserIdOrderByUpdatedAtDesc(currentUserService.requireCurrentUserId());
        List<WebSearchResult> results = loadWebResults(tasks).stream()
                .filter(item -> normalized.isBlank() || containsIgnoreCase(item.getTitle(), normalized) || containsIgnoreCase(item.getSnippetText(), normalized))
                .toList();
        if (platform == null || platform.isBlank() || "鍏ㄩ儴".equals(platform)) {
            return results;
        }
        return results.stream().filter(item -> matchPlatform(item.getPlatformName(), platform)).toList();
    }

    private List<PaperSearchResult> loadPaperResults(List<PaperSearchTask> tasks) {
        return tasks.stream()
                .flatMap(task -> paperSearchResultRepository.findByTaskIdOrderByCreatedAtAsc(task.getId()).stream())
                .sorted(Comparator.comparing(PaperSearchResult::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(20)
                .toList();
    }

    private List<WebSearchResult> loadWebResults(List<WebSearchTask> tasks) {
        return tasks.stream()
                .flatMap(task -> webSearchResultRepository.findByTaskIdOrderByCreatedAtAsc(task.getId()).stream())
                .sorted(Comparator.comparing(WebSearchResult::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(20)
                .toList();
    }

    private PaperSearchResult savePaperSearchResult(Long taskId, Map<String, Object> item) {
        PaperSearchResult result = new PaperSearchResult();
        result.setTaskId(taskId);
        result.setSourceName(String.valueOf(item.getOrDefault("source", "MetaSo")));
        result.setTitle(String.valueOf(item.getOrDefault("title", "未命名结果")));
        result.setAuthors(String.valueOf(item.getOrDefault("author", "")));
        result.setAbstractText(String.valueOf(item.getOrDefault("snippet", "")));
        result.setPaperUrl(String.valueOf(item.getOrDefault("url", "")));
        result.setPublishYear(LocalDate.now().getYear());
        result.setKeywordJson("[\"MetaSo\",\"实时搜索\"]");
        result.setRawJson(item.toString());
        result.setCitationCount(0);
        return paperSearchResultRepository.save(result);
    }

    private WebSearchResult saveWebSearchResult(Long taskId, Map<String, Object> item) {
        WebSearchResult result = new WebSearchResult();
        result.setTaskId(taskId);
        result.setPlatformName(String.valueOf(item.getOrDefault("source", "MetaSo")));
        result.setTitle(String.valueOf(item.getOrDefault("title", "未命名结果")));
        result.setAuthorName(String.valueOf(item.getOrDefault("author", "")));
        result.setUrl(String.valueOf(item.getOrDefault("url", "")));
        result.setSnippetText(String.valueOf(item.getOrDefault("snippet", "")));
        result.setMarkdownContent("- " + result.getTitle() + "：" + result.getSnippetText());
        result.setRawJson(item.toString());
        result.setPublishAt(LocalDateTime.now());
        return webSearchResultRepository.save(result);
    }

    private String buildWebMarkdown(String query, List<WebSearchResult> matches, String platform) {
        return """
                # 网页搜索简报

                - 关键词：%s
                - 平台：%s
                - 已聚合 %d 条线索
                - 建议把社区灵感、SOP 和工具导航分开沉淀，避免混入同一个资料夹。
                """.formatted(query, platform, matches.size());
    }

    private String enrichWebQuery(String query, String platform) {
        if (platform == null || platform.isBlank() || "全部".equals(platform)) {
            return query;
        }
        return query + " " + platform;
    }

    private boolean matchPlatform(String source, String platform) {
        if (platform == null || platform.isBlank() || "全部".equals(platform)) {
            return true;
        }
        return source != null && source.toLowerCase(Locale.ROOT).contains(platform.toLowerCase(Locale.ROOT));
    }

    private String generateAssistantReply(String prompt, List<String> sources) {
        String systemPrompt = """
                你是研究生科研工作台中的 AI 助手。
                你的输出必须围绕研究问题、资料沉淀、可执行计划和来源引用来组织。
                输出采用中文，优先给出可执行建议。
                """;
        String userPrompt = """
                用户问题：
                %s

                可参考来源：
                %s

                请给出结构化建议，包含：
                1. 重点判断
                2. 下一步动作
                3. 可沉淀到知识库的内容
                """.formatted(prompt, String.join("、", sources));
        try {
            return llmChatClient.chat(systemPrompt, userPrompt);
        } catch (Exception ex) {
            if (!appProperties.getExternal().isAllowFallback()) {
                throw ex;
            }
            return """
                    我建议把这项工作拆成三个动作：

                    1. 先完成研究问题标准化，明确检索边界和筛选字段。
                    2. 把高价值结果沉淀到知识库，并在摘要中保留来源片段。
                    3. 将 AI 输出反写成计划项，确保研究动作持续推进。

                    当前回答已优先参考：%s
                    """.formatted(String.join("、", sources));
        }
    }

    private List<Map<String, Object>> buildDashboardMetrics(Long userId, List<KnowledgeBase> knowledgeBases, List<PomodoroRecord> pomodoros) {
        int totalFocus = pomodoros.stream().mapToInt(PomodoroRecord::getFocusMinutes).sum();
        int knowledgeCount = knowledgeBases.stream().mapToInt(KnowledgeBase::getDocCount).sum();
        long todoCount = studyTaskRepository.countByUserIdAndTaskStatus(userId, "TODO");
        long outputCount = workspaceArtifactRepository.countByCreatorUserId(userId);
        return List.of(
                metric("今日专注", String.format(Locale.ROOT, "%.1fh", totalFocus / 60.0), "+18%"),
                metric("待完成计划", String.valueOf(todoCount), "本周持续推进"),
                metric("知识条目", String.valueOf(knowledgeCount), "来自真实数据库"),
                metric("AI 产出", String.valueOf(outputCount), "已落库")
        );
    }

    private List<Map<String, Object>> buildActivity(
            List<PaperSearchTask> recentPaperTasks,
            List<WebSearchTask> webTasks,
            List<AiChatMessage> messages
    ) {
        List<Map<String, Object>> activity = new ArrayList<>();
        recentPaperTasks.stream().limit(2).forEach(task -> activity.add(timeline(toTime(task.getCreatedAt()), "完成研究搜索：" + task.getQueryText())));
        webTasks.stream().limit(1).forEach(task -> activity.add(timeline(toTime(task.getUpdatedAt()), "保存网页简报：" + task.getQueryText())));
        messages.stream()
                .filter(message -> "ASSISTANT".equalsIgnoreCase(message.getRoleCode()))
                .limit(1)
                .forEach(message -> activity.add(timeline(toTime(message.getCreatedAt()), "生成知识库问答助手输出")));
        return activity.stream()
                .sorted(Comparator.comparing(item -> String.valueOf(item.get("time"))))
                .toList();
    }

    private List<Map<String, Object>> buildResearchInsights(List<PaperSearchResult> papers) {
        int yearMax = papers.stream().map(PaperSearchResult::getPublishYear).filter(year -> year != null).max(Integer::compareTo).orElse(LocalDate.now().getYear());
        return List.of(
                insight("趋势", "当前结果集中最新发表年份为 " + yearMax + "，项目空间与引用链是高频能力。"),
                insight("差异点", "真实结果已入库，可继续补做 Zotero/BibTeX 导入导出。"),
                insight("建议", "优先完成搜索 -> 入库 -> AI 输出 -> 计划落地的四步闭环。")
        );
    }

    private List<Map<String, Object>> buildPlanMetrics(Long userId, List<CheckinRecord> checkins, List<StudyTask> tasks, List<PomodoroRecord> pomodoros) {
        long streak = checkins.size();
        long doneCount = tasks.stream().filter(task -> "DONE".equalsIgnoreCase(task.getTaskStatus())).count();
        int totalPomodoro = pomodoros.size();
        return List.of(
                metric("连续打卡", streak + " 天", "保持中"),
                metric("本周完成", String.valueOf(doneCount), "任务推进"),
                metric("番茄钟", String.valueOf(totalPomodoro) + " 次", "累计 " + pomodoros.stream().mapToInt(PomodoroRecord::getFocusMinutes).sum() + " 分钟")
        );
    }

    private List<Integer> buildFocusTrend(Long userId) {
        return pomodoroRecordRepository.findByUserIdAndStartedAtGreaterThanEqualOrderByStartedAtAsc(userId, LocalDate.now().minusDays(6).atStartOfDay())
                .stream()
                .collect(Collectors.groupingBy(
                        record -> record.getStartedAt().toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.summingInt(PomodoroRecord::getFocusMinutes)
                ))
                .values()
                .stream()
                .toList();
    }

    private Map<String, Object> toRecentSearchItem(PaperSearchTask task) {
        return quickItem(task.getQueryText(), timeAgo(task.getCreatedAt()), task.getResultCount() + " 条结果");
    }

    private Map<String, Object> toTaskCard(StudyTask task) {
        return task(task.getTitle(), priorityLabel(task.getPriorityLevel()), task.getDueTime() == null ? "待安排" : deadlineLabel(task.getDueTime()));
    }

    private Map<String, Object> toProjectCard(ResearchProject project) {
        String progress = switch (project.getStageCode()) {
            case "WRITING" -> "72%";
            case "READING" -> "61%";
            case "SEARCH" -> "48%";
            default -> "30%";
        };
        return project(project.getName(), project.getTopic(), progress, stageLabel(project.getStageCode()));
    }

    private Map<String, Object> toPaperCard(PaperSearchResult result) {
        Map<String, Object> card = paper(
                result.getTitle(),
                result.getAuthors(),
                result.getSourceName(),
                String.valueOf(result.getPublishYear()),
                firstJsonTag(result.getKeywordJson(), "研究"),
                result.getAbstractText(),
                String.valueOf(result.getCitationCount() == null ? 0 : result.getCitationCount()),
                "真实结果"
        );
        card.put("id", result.getId());
        return card;
    }

    private Map<String, Object> toKnowledgeBaseCard(KnowledgeBase base) {
        return kb(base.getName(), base.getDocCount() + " 份资料 / " + base.getTotalChunkCount() + " 段切片");
    }

    private Map<String, Object> toKnowledgeDocumentCard(KnowledgeDocument document) {
        return document(document.getTitle(), document.getSourceType(), document.getParseStatus(), document.getUpdatedAt().toLocalDate().format(DATE_FORMATTER));
    }

    private Map<String, Object> toWebCard(WebSearchResult result) {
        Map<String, Object> card = web(
                result.getTitle(),
                result.getAuthorName(),
                result.getPlatformName(),
                result.getSnippetText(),
                firstJsonTag(result.getRawJson(), "网页")
        );
        card.put("id", result.getId());
        return card;
    }

    private Map<String, Object> toMessageCard(AiChatMessage message) {
        return message("ASSISTANT".equalsIgnoreCase(message.getRoleCode()) ? "assistant" : "user", message.getContentText());
    }

    private Map<String, Object> toCalendarCard(CalendarEvent event) {
        return calendar(dayLabel(event.getStartTime()), event.getTitle());
    }

    private String toBindingLabel(UserSocialBinding binding) {
        return switch (binding.getPlatform()) {
            case "wechat" -> "微信已绑定";
            case "feishu" -> "飞书待增强";
            case "github" -> "GitHub 已绑定";
            default -> binding.getPlatform() + " 已绑定";
        };
    }

    private String priorityLabel(Integer priorityLevel) {
        if (priorityLevel == null) {
            return "中优先级";
        }
        return switch (priorityLevel) {
            case 1 -> "高优先级";
            case 3 -> "低优先级";
            default -> "中优先级";
        };
    }

    private String deadlineLabel(LocalDateTime dueTime) {
        LocalDate date = dueTime.toLocalDate();
        if (date.equals(LocalDate.now())) {
            return "今天 " + dueTime.toLocalTime().withSecond(0).withNano(0);
        }
        if (date.equals(LocalDate.now().plusDays(1))) {
            return "明天 " + dueTime.toLocalTime().withSecond(0).withNano(0);
        }
        return dueTime.toLocalDate().format(DATE_FORMATTER);
    }

    private String composeRole(String degreeLevel, String researchDirection) {
        String left = degreeLevel == null ? "" : degreeLevel.trim();
        String right = researchDirection == null ? "" : researchDirection.trim();
        if (!left.isBlank() && !right.isBlank()) {
            return left + " / " + right;
        }
        if (!left.isBlank()) {
            return left;
        }
        if (!right.isBlank()) {
            return right;
        }
        return "研究者";
    }

    private String formatTodayUsage(int minutes) {
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;
        return String.format("%02d:%02d", hours, remainingMinutes);
    }

    private int normalizeGender(Integer gender) {
        if (gender == null) {
            return 0;
        }
        return (gender >= 0 && gender <= 2) ? gender : 0;
    }

    private String nullToEmpty(String text) {
        return text == null ? "" : text;
    }

    private String firstNonBlank(String preferred, String fallback) {
        String normalizedPreferred = preferred == null ? "" : preferred.trim();
        if (!normalizedPreferred.isBlank()) {
            return normalizedPreferred;
        }
        return nullToEmpty(fallback);
    }

    private String stageLabel(String stageCode) {
        return switch (stageCode) {
            case "WRITING" -> "正在撰写阶段";
            case "READING" -> "资料沉淀中";
            case "SEARCH" -> "等待数据回填";
            default -> "想法孵化中";
        };
    }

    private String dayLabel(LocalDateTime dateTime) {
        String[] labels = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return labels[dateTime.getDayOfWeek().getValue() - 1];
    }

    private String timeAgo(LocalDateTime time) {
        if (time == null) {
            return "刚刚";
        }
        long hours = java.time.Duration.between(time, LocalDateTime.now()).toHours();
        if (hours <= 0) {
            long minutes = Math.max(1, java.time.Duration.between(time, LocalDateTime.now()).toMinutes());
            return minutes + " 分钟前";
        }
        return hours + " 小时前";
    }

    private String toTime(LocalDateTime time) {
        if (time == null) {
            return "00:00";
        }
        return time.toLocalTime().withSecond(0).withNano(0).toString();
    }

    private String firstJsonTag(String json, String fallback) {
        List<String> values = parseJsonArray(json);
        if (!values.isEmpty()) {
            return values.get(0);
        }
        if (json != null && json.contains("badge")) {
            int marker = json.indexOf(':');
            if (marker > 0 && json.endsWith("}")) {
                return json.substring(marker + 2, json.length() - 2);
            }
        }
        return fallback;
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        String trimmed = json.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        if (trimmed.isBlank()) {
            return List.of();
        }
        return List.of(trimmed.split(","))
                .stream()
                .map(item -> item.trim().replace("\"", ""))
                .filter(item -> !item.isBlank())
                .toList();
    }

    private String toJsonArray(List<String> values) {
        return values.stream().map(value -> "\"" + value + "\"").collect(Collectors.joining(",", "[", "]"));
    }

    private String normalize(String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }

    private boolean containsIgnoreCase(String text, String keyword) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private Map<String, Object> module(String id, String label, String description) {
        return linkedMap("id", id, "label", label, "description", description);
    }

    private Map<String, Object> metric(String label, String value, String detail) {
        return linkedMap("label", label, "value", value, "detail", detail);
    }

    private Map<String, Object> quickItem(String title, String time, String meta) {
        return linkedMap("title", title, "time", time, "meta", meta);
    }

    private Map<String, Object> task(String title, String priority, String deadline) {
        return linkedMap("title", title, "priority", priority, "deadline", deadline);
    }

    private Map<String, Object> project(String title, String field, String progress, String stage) {
        return linkedMap("title", title, "field", field, "progress", progress, "stage", stage);
    }

    private Map<String, Object> timeline(String time, String text) {
        return linkedMap("time", time, "text", text);
    }

    private Map<String, Object> paper(
            String title,
            String authors,
            String source,
            String year,
            String badge,
            String summary,
            String citations,
            String tag
    ) {
        return linkedMap(
                "title", title,
                "authors", authors,
                "source", source,
                "year", year,
                "badge", badge,
                "summary", summary,
                "citations", citations,
                "tag", tag
        );
    }

    private Map<String, Object> insight(String title, String text) {
        return linkedMap("title", title, "text", text);
    }

    private Map<String, Object> kb(String name, String meta) {
        return linkedMap("name", name, "meta", meta);
    }

    private Map<String, Object> document(String title, String source, String status, String date) {
        return linkedMap("title", title, "source", source, "status", status, "date", date);
    }

    private Map<String, Object> web(String title, String author, String platform, String summary, String badge) {
        return linkedMap(
                "title", title,
                "author", author,
                "platform", platform,
                "summary", summary,
                "badge", badge
        );
    }

    private Map<String, Object> assistant(String title, String summary) {
        return linkedMap("title", title, "summary", summary);
    }

    private Map<String, Object> message(String role, String content) {
        return linkedMap("role", role, "content", content);
    }

    private Map<String, Object> calendar(String day, String text) {
        return linkedMap("day", day, "text", text);
    }

    private Map<String, Object> linkedMap(Object... values) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }
}
