package com.research.workbench.bootstrap;

import com.research.workbench.domain.AiChatMessage;
import com.research.workbench.domain.AiChatSession;
import com.research.workbench.domain.CalendarEvent;
import com.research.workbench.domain.CheckinRecord;
import com.research.workbench.domain.KnowledgeBase;
import com.research.workbench.domain.KnowledgeChunk;
import com.research.workbench.domain.KnowledgeDocument;
import com.research.workbench.domain.PaperSearchResult;
import com.research.workbench.domain.PaperSearchTask;
import com.research.workbench.domain.PomodoroRecord;
import com.research.workbench.domain.ResearchProject;
import com.research.workbench.domain.StudyTask;
import com.research.workbench.domain.SysUser;
import com.research.workbench.domain.UserProfile;
import com.research.workbench.domain.UserSocialBinding;
import com.research.workbench.domain.WebSearchResult;
import com.research.workbench.domain.WebSearchTask;
import com.research.workbench.domain.Workspace;
import com.research.workbench.domain.WorkspaceArtifact;
import com.research.workbench.repository.AiChatMessageRepository;
import com.research.workbench.repository.AiChatSessionRepository;
import com.research.workbench.repository.CalendarEventRepository;
import com.research.workbench.repository.CheckinRecordRepository;
import com.research.workbench.repository.KnowledgeBaseRepository;
import com.research.workbench.repository.KnowledgeChunkRepository;
import com.research.workbench.repository.KnowledgeDocumentRepository;
import com.research.workbench.repository.PaperSearchResultRepository;
import com.research.workbench.repository.PaperSearchTaskRepository;
import com.research.workbench.repository.PomodoroRecordRepository;
import com.research.workbench.repository.ResearchProjectRepository;
import com.research.workbench.repository.StudyTaskRepository;
import com.research.workbench.repository.SysUserRepository;
import com.research.workbench.repository.UserProfileRepository;
import com.research.workbench.repository.UserSocialBindingRepository;
import com.research.workbench.repository.WebSearchResultRepository;
import com.research.workbench.repository.WebSearchTaskRepository;
import com.research.workbench.repository.WorkspaceArtifactRepository;
import com.research.workbench.repository.WorkspaceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoDataInitializer implements CommandLineRunner {

    private final SysUserRepository sysUserRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserSocialBindingRepository userSocialBindingRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ResearchProjectRepository researchProjectRepository;
    private final PaperSearchTaskRepository paperSearchTaskRepository;
    private final PaperSearchResultRepository paperSearchResultRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final WebSearchTaskRepository webSearchTaskRepository;
    private final WebSearchResultRepository webSearchResultRepository;
    private final AiChatSessionRepository aiChatSessionRepository;
    private final AiChatMessageRepository aiChatMessageRepository;
    private final WorkspaceArtifactRepository workspaceArtifactRepository;
    private final StudyTaskRepository studyTaskRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final PomodoroRecordRepository pomodoroRecordRepository;
    private final CheckinRecordRepository checkinRecordRepository;

    public DemoDataInitializer(
            SysUserRepository sysUserRepository,
            UserProfileRepository userProfileRepository,
            UserSocialBindingRepository userSocialBindingRepository,
            WorkspaceRepository workspaceRepository,
            ResearchProjectRepository researchProjectRepository,
            PaperSearchTaskRepository paperSearchTaskRepository,
            PaperSearchResultRepository paperSearchResultRepository,
            KnowledgeBaseRepository knowledgeBaseRepository,
            KnowledgeDocumentRepository knowledgeDocumentRepository,
            KnowledgeChunkRepository knowledgeChunkRepository,
            WebSearchTaskRepository webSearchTaskRepository,
            WebSearchResultRepository webSearchResultRepository,
            AiChatSessionRepository aiChatSessionRepository,
            AiChatMessageRepository aiChatMessageRepository,
            WorkspaceArtifactRepository workspaceArtifactRepository,
            StudyTaskRepository studyTaskRepository,
            CalendarEventRepository calendarEventRepository,
            PomodoroRecordRepository pomodoroRecordRepository,
            CheckinRecordRepository checkinRecordRepository
    ) {
        this.sysUserRepository = sysUserRepository;
        this.userProfileRepository = userProfileRepository;
        this.userSocialBindingRepository = userSocialBindingRepository;
        this.workspaceRepository = workspaceRepository;
        this.researchProjectRepository = researchProjectRepository;
        this.paperSearchTaskRepository = paperSearchTaskRepository;
        this.paperSearchResultRepository = paperSearchResultRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.webSearchTaskRepository = webSearchTaskRepository;
        this.webSearchResultRepository = webSearchResultRepository;
        this.aiChatSessionRepository = aiChatSessionRepository;
        this.aiChatMessageRepository = aiChatMessageRepository;
        this.workspaceArtifactRepository = workspaceArtifactRepository;
        this.studyTaskRepository = studyTaskRepository;
        this.calendarEventRepository = calendarEventRepository;
        this.pomodoroRecordRepository = pomodoroRecordRepository;
        this.checkinRecordRepository = checkinRecordRepository;
    }

    @Override
    public void run(String... args) {
        if (sysUserRepository.count() > 0) {
            return;
        }

        SysUser user = new SysUser();
        user.setUsername("researcher");
        user.setPasswordHash("{noop}researcher");
        user.setEmail("researcher@example.com");
        user.setNickname("实验室研究者");
        user.setAvatarUrl("/favicon.svg");
        user.setLastLoginAt(LocalDateTime.now().minusHours(2));
        user = sysUserRepository.save(user);

        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        profile.setRealName("实验室研究者");
        profile.setBio("关注肿瘤多组学分析、科研工作流设计与研究效率工具。");
        profile.setInstitution("Shenyang Pharmaceutical University");
        profile.setDepartment("生物信息学实验室");
        profile.setResearchDirection("肿瘤 biomarker、RAG、科研工作流");
        profile.setDegreeLevel("Master");
        profile.setInterests(jsonArray("Biomarker", "Bioinformatics", "Workflow"));
        profile.setTags(jsonArray("Biomarker", "RAG", "Workflow", "Research UX"));
        userProfileRepository.save(profile);

        userSocialBindingRepository.saveAll(List.of(
                binding(user.getId(), "wechat", "wechat-openid-demo"),
                binding(user.getId(), "github", "github-openid-demo"),
                binding(user.getId(), "feishu", "feishu-openid-demo")
        ));

        Workspace workspace = new Workspace();
        workspace.setOwnerUserId(user.getId());
        workspace.setName("研究生科研工作台");
        workspace.setDescription("把检索、知识沉淀、AI 产出和计划执行统一到一个项目空间。");
        workspace.setVisibility("PRIVATE");
        workspace.setSystemPrompt("围绕研究问题驱动检索、沉淀、输出和计划推进。");
        workspace = workspaceRepository.save(workspace);

        ResearchProject project1 = new ResearchProject();
        project1.setWorkspaceId(workspace.getId());
        project1.setOwnerUserId(user.getId());
        project1.setName("生物标志物发现");
        project1.setTopic("多组学 / 生信");
        project1.setSummary("围绕肿瘤 biomarker 发现与 explainable evidence chain 建立资料体系。");
        project1.setColorToken("ocean");
        project1.setStageCode("WRITING");
        project1.setDueDate(LocalDate.now().plusDays(7));
        project1 = researchProjectRepository.save(project1);

        ResearchProject project2 = new ResearchProject();
        project2.setWorkspaceId(workspace.getId());
        project2.setOwnerUserId(user.getId());
        project2.setName("科研工作台产品设计");
        project2.setTopic("产品 / 交互");
        project2.setSummary("研究项目空间、知识库与 AI 工作流的一体化产品设计。");
        project2.setColorToken("violet");
        project2.setStageCode("SEARCH");
        project2.setDueDate(LocalDate.now().plusDays(10));
        project2 = researchProjectRepository.save(project2);

        ResearchProject project3 = new ResearchProject();
        project3.setWorkspaceId(workspace.getId());
        project3.setOwnerUserId(user.getId());
        project3.setName("实验室资料规范");
        project3.setTopic("协作 / 知识管理");
        project3.setSummary("沉淀实验室 SOP、命名规范和资料入库标准。");
        project3.setColorToken("mint");
        project3.setStageCode("READING");
        project3.setDueDate(LocalDate.now().plusDays(14));
        project3 = researchProjectRepository.save(project3);

        PaperSearchTask paperTask = new PaperSearchTask();
        paperTask.setUserId(user.getId());
        paperTask.setWorkspaceId(workspace.getId());
        paperTask.setProjectId(project2.getId());
        paperTask.setQueryText("研究生如何把学术检索、知识沉淀与 AI 工作流整合成可持续系统？");
        paperTask.setResultCount(3);
        paperTask.setStartedAt(LocalDateTime.now().minusHours(5));
        paperTask.setFinishedAt(LocalDateTime.now().minusHours(5).plusMinutes(2));
        paperTask = paperSearchTaskRepository.save(paperTask);

        paperSearchResultRepository.saveAll(List.of(
                paperResult(
                        paperTask.getId(),
                        "Nature Communications",
                        "多组学整合驱动的肿瘤生物标志物发现框架",
                        "张琳, Wang Q., He Y.",
                        "整合转录组、甲基化与临床结局，支持 biomarker ranking、evidence chain 和临床分层解释。",
                        2025,
                        "方法学 / 生信",
                        89,
                        new BigDecimal("0.96")
                ),
                paperResult(
                        paperTask.getId(),
                        "Briefings in Bioinformatics",
                        "面向系统综述的 AI-assisted 文献证据汇总流程",
                        "Liu X., Chen F.",
                        "强调研究问题驱动检索、证据分桶、句级引用与快速 brief 输出。",
                        2024,
                        "综述 / Workflow",
                        56,
                        new BigDecimal("0.91")
                ),
                paperResult(
                        paperTask.getId(),
                        "CHI Extended Abstracts",
                        "Research Workspace Design for Graduate Labs",
                        "Marin A., Zhao K.",
                        "提出项目空间、资料归档、AI 协作与专注管理融合的科研工作台模式。",
                        2024,
                        "HCI / 产品",
                        24,
                        new BigDecimal("0.88")
                )
        ));

        KnowledgeBase kb1 = knowledgeBase("癌症 biomarker", "核心论文与证据片段集合", workspace.getId(), user.getId(), 96, 420);
        KnowledgeBase kb2 = knowledgeBase("科研工作台产品", "竞品、架构与交互资料", workspace.getId(), user.getId(), 38, 126);
        KnowledgeBase kb3 = knowledgeBase("实验室流程 SOP", "流程规范、命名约束与模板", workspace.getId(), user.getId(), 19, 56);
        kb1 = knowledgeBaseRepository.save(kb1);
        kb2 = knowledgeBaseRepository.save(kb2);
        kb3 = knowledgeBaseRepository.save(kb3);

        List<KnowledgeDocument> seededDocs = knowledgeDocumentRepository.saveAll(List.of(
                knowledgeDocument(kb1.getId(), user.getId(), "论文", "多组学综述.pdf", "pdf", "PARSED", 2),
                knowledgeDocument(kb2.getId(), user.getId(), "网页资料", "竞品调研.md", "md", "PARSED", 2),
                knowledgeDocument(kb3.getId(), user.getId(), "内部文档", "实验流程规范.docx", "docx", "PARSED", 1)
        ));
        seededDocs.forEach(document -> knowledgeChunkRepository.saveAll(seedChunks(document)));

        WebSearchTask webTask = new WebSearchTask();
        webTask.setUserId(user.getId());
        webTask.setWorkspaceId(workspace.getId());
        webTask.setProjectId(project2.getId());
        webTask.setQueryText("MetaSo 小红书 科研工具");
        webTask.setPlatformScope("webpage");
        webTask.setResultCount(3);
        webTask.setMarkdownSummary("""
                # 科研工作流网页观察

                - 用户关注的不只是检索速度，更在意是否可以直接保存并继续提问。
                - 视频和社区内容更适合启发式发现，正式沉淀仍然需要 Markdown 和知识库。
                - 项目空间、模板化输出和证据回链是高价值差异点。
                """);
        webTask = webSearchTaskRepository.save(webTask);

        webSearchResultRepository.saveAll(List.of(
                webResult(webTask.getId(), "小红书", "小红书科研效率工具盘点", "小研同学", "把 Zotero、NotebookLM、Perplexity 和笔记流串成一个闭环，适合研究生快速搭工作流。", "内容沉淀"),
                webResult(webTask.getId(), "独立站点", "实验室网页资料整理 SOP", "BioOps 团队", "从数据检索、资料命名、Markdown 摘要到入库标签，形成统一实验室规范。", "流程规范"),
                webResult(webTask.getId(), "抖音", "抖音: 医学论文快速总结演示", "医学生阿杰", "演示如何基于论文摘要、图表和知识库做多轮提问并沉淀结论。", "短视频灵感")
        ));

        AiChatSession session = new AiChatSession();
        session.setUserId(user.getId());
        session.setWorkspaceId(workspace.getId());
        session.setProjectId(project2.getId());
        session.setTitle("科研工作流周计划");
        session.setAssistantType("GENERAL");
        session.setSourceDocIds(jsonArray("竞品调研.md", "多组学综述.pdf", "Research Workspace Design 2024"));
        session = aiChatSessionRepository.save(session);

        aiChatMessageRepository.saveAll(List.of(
                message(session.getId(), "ASSISTANT", "我已经根据你的研究方向整理出 3 个重点主题，并附上引用来源。"),
                message(session.getId(), "USER", "请把这些主题转成下周的研究计划。"),
                message(session.getId(), "ASSISTANT", "已拆分为 4 个可执行任务，并补充了每个任务的输入资料。")
        ));

        AiChatSession kbSession = new AiChatSession();
        kbSession.setUserId(user.getId());
        kbSession.setWorkspaceId(workspace.getId());
        kbSession.setTitle("科研工作台知识库问答");
        kbSession.setAssistantType("KB");
        kbSession.setSourceDocIds("[\"KB:" + kb2.getId() + "\"]");
        kbSession = aiChatSessionRepository.save(kbSession);

        aiChatMessageRepository.saveAll(List.of(
                kbMessage(kbSession.getId(), "USER", "知识库里关于 research workspace 的核心差异点是什么？"),
                kbMessage(kbSession.getId(), "ASSISTANT", "当前资料显示，项目空间、引用回链和模板化输出是最稳定的三项差异能力。")
        ));

        workspaceArtifactRepository.saveAll(List.of(
                artifact(workspace.getId(), project1.getId(), user.getId(), "REPORT", "肿瘤 biomarker 周报"),
                artifact(workspace.getId(), project2.getId(), user.getId(), "MARKDOWN", "竞品功能拆解"),
                artifact(workspace.getId(), project2.getId(), user.getId(), "BRIEFING", "研究工作台 brief"),
                artifact(workspace.getId(), project3.getId(), user.getId(), "REPORT", "实验室 SOP 草稿")
        ));

        studyTaskRepository.saveAll(List.of(
                task(user.getId(), workspace.getId(), project1.getId(), "完成肿瘤 biomarker brief v1", "高优先级任务", 1, LocalDateTime.now().withHour(18).withMinute(0)),
                task(user.getId(), workspace.getId(), project2.getId(), "整理小红书竞品观察入知识库", "同步网页搜索与知识库", 2, LocalDateTime.now().withHour(21).withMinute(0)),
                task(user.getId(), workspace.getId(), project2.getId(), "完善项目空间权限设计", "补充成员角色与共享边界", 2, LocalDateTime.now().plusDays(1).withHour(10).withMinute(0)),
                task(user.getId(), workspace.getId(), project2.getId(), "设计 Web 搜索保存为 Markdown 的导出入口", "补导出与回写路径", 2, LocalDateTime.now().plusDays(4))
        ));

        calendarEventRepository.saveAll(List.of(
                calendar(user.getId(), workspace.getId(), project2.getId(), "完成 MVP 页面骨架", 0),
                calendar(user.getId(), workspace.getId(), project2.getId(), "联调研究检索与知识库", 1),
                calendar(user.getId(), workspace.getId(), project2.getId(), "接入 AI 输出保存", 2),
                calendar(user.getId(), workspace.getId(), project2.getId(), "补番茄钟与统计卡片", 3)
        ));

        pomodoroRecordRepository.saveAll(List.of(
                pomodoro(user.getId(), 45, 6),
                pomodoro(user.getId(), 60, 5),
                pomodoro(user.getId(), 30, 4),
                pomodoro(user.getId(), 75, 3),
                pomodoro(user.getId(), 90, 2),
                pomodoro(user.getId(), 65, 1),
                pomodoro(user.getId(), 80, 0)
        ));

        checkinRecordRepository.saveAll(List.of(
                checkin(user.getId(), 6, 80, 3),
                checkin(user.getId(), 5, 65, 2),
                checkin(user.getId(), 4, 90, 4),
                checkin(user.getId(), 3, 75, 3),
                checkin(user.getId(), 2, 30, 1),
                checkin(user.getId(), 1, 60, 2),
                checkin(user.getId(), 0, 45, 2)
        ));
    }

    private UserSocialBinding binding(Long userId, String platform, String openId) {
        UserSocialBinding binding = new UserSocialBinding();
        binding.setUserId(userId);
        binding.setPlatform(platform);
        binding.setOpenId(openId);
        binding.setBoundAt(LocalDateTime.now().minusDays(7));
        return binding;
    }

    private PaperSearchResult paperResult(
            Long taskId,
            String sourceName,
            String title,
            String authors,
            String summary,
            Integer year,
            String badge,
            Integer citations,
            BigDecimal score
    ) {
        PaperSearchResult result = new PaperSearchResult();
        result.setTaskId(taskId);
        result.setSourceName(sourceName);
        result.setTitle(title);
        result.setAuthors(authors);
        result.setAbstractText(summary);
        result.setPublishYear(year);
        result.setJournalName(sourceName);
        result.setCitationCount(citations);
        result.setKeywordJson(jsonArray(badge));
        result.setScore(score);
        return result;
    }

    private KnowledgeBase knowledgeBase(
            String name,
            String description,
            Long workspaceId,
            Long userId,
            Integer docCount,
            Integer chunkCount
    ) {
        KnowledgeBase base = new KnowledgeBase();
        base.setName(name);
        base.setDescription(description);
        base.setWorkspaceId(workspaceId);
        base.setOwnerUserId(userId);
        base.setDocCount(docCount);
        base.setTotalChunkCount(chunkCount);
        return base;
    }

    private KnowledgeDocument knowledgeDocument(
            Long kbId,
            Long userId,
            String sourceType,
            String fileName,
            String fileExt,
            String status,
            Integer chunkCount
    ) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setKbId(kbId);
        document.setUploadedBy(userId);
        document.setSourceType(sourceType);
        document.setTitle(fileName);
        document.setFileName(fileName);
        document.setFileExt(fileExt);
        document.setParseStatus(status);
        document.setChunkCount(chunkCount);
        document.setTagJson(jsonArray(sourceType, status));
        document.setSummaryText("已沉淀到知识库，可继续问答与引用回溯。");
        return document;
    }

    private List<KnowledgeChunk> seedChunks(KnowledgeDocument document) {
        List<String> contents = switch (document.getTitle()) {
            case "多组学综述.pdf" -> List.of(
                    "多组学 biomarker 分析强调证据链和临床可解释性，不能只给排序结果，还要返回支撑片段。",
                    "研究工作流应把检索结果、知识库切片和最终输出连成闭环，避免结论脱离原始资料。"
            );
            case "竞品调研.md" -> List.of(
                    "项目空间、引用回链、模板化输出是当前研究产品的核心差异点。",
                    "如果用户能把搜索结果一键存入知识库，再从知识库发起问答，产品粘性会明显提升。"
            );
            default -> List.of("实验室流程规范应统一命名、上传、引用和版本管理方式。");
        };
        List<KnowledgeChunk> chunks = new ArrayList<>();
        for (int i = 0; i < contents.size(); i++) {
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setDocumentId(document.getId());
            chunk.setChunkNo(i + 1);
            chunk.setContentText(contents.get(i));
            chunk.setTokenCount(Math.max(1, contents.get(i).length() / 4));
            chunk.setKeywordJson("[\"知识库\",\"引用\"]");
            chunks.add(chunk);
        }
        return chunks;
    }

    private WebSearchResult webResult(
            Long taskId,
            String platform,
            String title,
            String author,
            String snippet,
            String badge
    ) {
        WebSearchResult result = new WebSearchResult();
        result.setTaskId(taskId);
        result.setPlatformName(platform);
        result.setTitle(title);
        result.setAuthorName(author);
        result.setSnippetText(snippet);
        result.setMarkdownContent("- " + title + "：" + snippet);
        result.setScore(new BigDecimal("0.82"));
        result.setRawJson("{\"badge\":\"" + badge + "\"}");
        result.setPublishAt(LocalDateTime.now().minusHours(4));
        return result;
    }

    private AiChatMessage message(Long sessionId, String role, String content) {
        AiChatMessage message = new AiChatMessage();
        message.setSessionId(sessionId);
        message.setRoleCode(role);
        message.setContentText(content);
        message.setModelName("qwen-plus");
        message.setSourceRefs(jsonArray("竞品调研.md", "多组学综述.pdf"));
        return message;
    }

    private AiChatMessage kbMessage(Long sessionId, String role, String content) {
        AiChatMessage message = new AiChatMessage();
        message.setSessionId(sessionId);
        message.setRoleCode(role);
        message.setContentText(content);
        message.setModelName("knowledge-rag");
        message.setSourceRefs("[{\"documentTitle\":\"竞品调研.md\",\"chunkNo\":1,\"excerpt\":\"项目空间、引用回链、模板化输出是当前研究产品的核心差异点。\"}]");
        return message;
    }

    private WorkspaceArtifact artifact(Long workspaceId, Long projectId, Long userId, String type, String title) {
        WorkspaceArtifact artifact = new WorkspaceArtifact();
        artifact.setWorkspaceId(workspaceId);
        artifact.setProjectId(projectId);
        artifact.setCreatorUserId(userId);
        artifact.setArtifactType(type);
        artifact.setTitle(title);
        artifact.setSourceType("MIXED");
        artifact.setSourceRefs(jsonArray("paper", "web", "kb"));
        artifact.setContentMarkdown("# " + title + "\n\n这是自动初始化的工作台产物。");
        return artifact;
    }

    private StudyTask task(
            Long userId,
            Long workspaceId,
            Long projectId,
            String title,
            String description,
            Integer priority,
            LocalDateTime dueTime
    ) {
        StudyTask task = new StudyTask();
        task.setUserId(userId);
        task.setWorkspaceId(workspaceId);
        task.setProjectId(projectId);
        task.setTitle(title);
        task.setDescription(description);
        task.setPriorityLevel(priority);
        task.setDueTime(dueTime);
        return task;
    }

    private CalendarEvent calendar(Long userId, Long workspaceId, Long projectId, String title, int offsetDays) {
        CalendarEvent event = new CalendarEvent();
        event.setUserId(userId);
        event.setWorkspaceId(workspaceId);
        event.setProjectId(projectId);
        event.setTitle(title);
        event.setDescription(title);
        event.setStartTime(LocalDate.now().plusDays(offsetDays).atTime(10, 0));
        event.setEndTime(LocalDate.now().plusDays(offsetDays).atTime(11, 0));
        return event;
    }

    private PomodoroRecord pomodoro(Long userId, int focusMinutes, int offsetDays) {
        PomodoroRecord record = new PomodoroRecord();
        record.setUserId(userId);
        record.setFocusMinutes(focusMinutes);
        record.setBreakMinutes(5);
        record.setStatus("DONE");
        record.setStartedAt(LocalDate.now().minusDays(offsetDays).atTime(9, 0));
        record.setFinishedAt(LocalDate.now().minusDays(offsetDays).atTime(9, 0).plusMinutes(focusMinutes));
        record.setCreatedAt(LocalDate.now().minusDays(offsetDays).atTime(9, 0));
        return record;
    }

    private CheckinRecord checkin(Long userId, int offsetDays, int focusMinutes, int completedCount) {
        CheckinRecord record = new CheckinRecord();
        record.setUserId(userId);
        record.setCheckinDate(LocalDate.now().minusDays(offsetDays));
        record.setFocusMinutes(focusMinutes);
        record.setCompletedTaskCount(completedCount);
        record.setSummaryText("保持研究节奏，持续推进核心任务。");
        record.setMoodCode("GOOD");
        record.setCreatedAt(LocalDate.now().minusDays(offsetDays).atTime(22, 0));
        return record;
    }

    private String jsonArray(String... values) {
        return "[" + String.join(",", List.of(values).stream().map(value -> "\"" + value + "\"").toList()) + "]";
    }
}
