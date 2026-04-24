package com.research.workbench.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.research.workbench.service.CurrentUserService;
import com.research.workbench.domain.AiChatMessage;
import com.research.workbench.domain.AiChatSession;
import com.research.workbench.domain.KnowledgeBase;
import com.research.workbench.domain.SysUser;
import com.research.workbench.domain.Workspace;
import com.research.workbench.service.SearchHistoryService;
import com.research.workbench.repository.AiChatMessageRepository;
import com.research.workbench.repository.AiChatSessionRepository;
import com.research.workbench.repository.KnowledgeBaseRepository;
import com.research.workbench.repository.WorkspaceRepository;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class KnowledgeConversationService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AiChatSessionRepository aiChatSessionRepository;
    private final AiChatMessageRepository aiChatMessageRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeRagService knowledgeRagService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final ObjectMapper objectMapper;
    private final CurrentUserService currentUserService;
    private final WorkspaceRepository workspaceRepository;
    private final SearchHistoryService searchHistoryService;

    public KnowledgeConversationService(
            AiChatSessionRepository aiChatSessionRepository,
            AiChatMessageRepository aiChatMessageRepository,
            KnowledgeBaseRepository knowledgeBaseRepository,
            KnowledgeRagService knowledgeRagService,
            KnowledgeBaseService knowledgeBaseService,
            ObjectMapper objectMapper,
            CurrentUserService currentUserService,
            WorkspaceRepository workspaceRepository,
            SearchHistoryService searchHistoryService
    ) {
        this.aiChatSessionRepository = aiChatSessionRepository;
        this.aiChatMessageRepository = aiChatMessageRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.knowledgeRagService = knowledgeRagService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.objectMapper = objectMapper;
        this.currentUserService = currentUserService;
        this.workspaceRepository = workspaceRepository;
        this.searchHistoryService = searchHistoryService;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listSessions(Long kbId) {
        requireBase(kbId);
        return aiChatSessionRepository
                .findByUserIdAndAssistantTypeAndSourceDocIdsContainingOrderByUpdatedAtDesc(currentUserService.requireCurrentUserId(), "KB", kbMarker(kbId))
                .stream()
                .map(this::toSessionDto)
                .toList();
    }

    public Map<String, Object> createSession(Long kbId, String title) {
        KnowledgeBase base = requireBase(kbId);
        SysUser user = currentUserService.requireCurrentUser();
        AiChatSession session = new AiChatSession();
        session.setUserId(user.getId());
        session.setWorkspaceId(requireWorkspaceId());
        session.setTitle(StringUtils.hasText(title) ? title : base.getName() + " 问答会话");
        session.setAssistantType("KB");
        session.setSourceDocIds("[\"" + kbMarker(kbId) + "\"]");
        session = aiChatSessionRepository.save(session);
        searchHistoryService.ensureSession(
                "KB-" + session.getId(),
                user.getId(),
                user.getNickname(),
                "KB_CHAT",
                session.getTitle()
        );
        return toSessionDto(session);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listMessages(Long sessionId) {
        AiChatSession session = requireSession(sessionId);
        return aiChatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId())
                .stream()
                .map(this::toMessageDto)
                .toList();
    }

    public Map<String, Object> ask(Long sessionId, String question) {
        AiChatSession session = requireSession(sessionId);
        SysUser user = currentUserService.requireCurrentUser();
        Long kbId = extractKbId(session);

        if (!StringUtils.hasText(session.getTitle()) || session.getTitle().contains("问答会话")) {
            session.setTitle(autoTitle(question));
        }

        AiChatMessage userMessage = new AiChatMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setRoleCode("USER");
        userMessage.setContentText(question);
        aiChatMessageRepository.save(userMessage);

        List<String> history = aiChatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(AiChatMessage::getContentText)
                .toList();

        Map<String, Object> ragResult = knowledgeRagService.answerQuestion(kbId, question, history);
        AiChatMessage assistantMessage = new AiChatMessage();
        assistantMessage.setSessionId(sessionId);
        assistantMessage.setRoleCode("ASSISTANT");
        assistantMessage.setContentText(String.valueOf(ragResult.get("answer")));
        assistantMessage.setModelName("knowledge-rag");
        assistantMessage.setSourceRefs(String.valueOf(ragResult.get("sourceRefsJson")));
        assistantMessage = aiChatMessageRepository.save(assistantMessage);

        Map<String, Object> response = Map.of(
                "session", toSessionDto(aiChatSessionRepository.save(session)),
                "message", toMessageDto(assistantMessage),
                "citations", ragResult.get("citations")
        );
        searchHistoryService.appendDetail(
                "KB-" + session.getId(),
                user.getId(),
                user.getNickname(),
                "KB_CHAT",
                session.getTitle(),
                question,
                ragResult.get("answer"),
                Map.of(
                        "citations", ragResult.get("citations"),
                        "kbId", kbId,
                        "aiSessionId", session.getId()
                )
        );
        return response;
    }

    public Map<String, Object> renameSession(Long sessionId, String title) {
        AiChatSession session = requireSession(sessionId);
        session.setTitle(StringUtils.hasText(title) ? title : session.getTitle());
        session = aiChatSessionRepository.save(session);
        return toSessionDto(session);
    }

    public void deleteSession(Long sessionId) {
        AiChatSession session = requireSession(sessionId);
        aiChatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .forEach(aiChatMessageRepository::delete);
        aiChatSessionRepository.delete(session);
    }

    public Map<String, Object> autoTitleSession(Long sessionId) {
        AiChatSession session = requireSession(sessionId);
        List<AiChatMessage> messages = aiChatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        String seed = messages.stream()
                .map(AiChatMessage::getContentText)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("知识库问答");
        session.setTitle(autoTitle(seed));
        session = aiChatSessionRepository.save(session);
        return toSessionDto(session);
    }

    public Map<String, Object> saveAssistantMessageToKnowledge(Long sessionId, Long messageId, String title) {
        AiChatSession session = requireSession(sessionId);
        Long kbId = extractKbId(session);
        AiChatMessage message = aiChatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("消息不存在: " + messageId));
        if (!sessionId.equals(message.getSessionId())) {
            throw new IllegalArgumentException("消息不属于当前会话");
        }
        if (!"ASSISTANT".equalsIgnoreCase(message.getRoleCode())) {
            throw new IllegalArgumentException("只能保存助手回答");
        }

        String docTitle = StringUtils.hasText(title) ? title : "会话回答-" + messageId;
        Map<String, Object> document = knowledgeBaseService.createTextDocument(kbId, docTitle, message.getContentText(), "AI");
        return Map.of(
                "savedDocument", document,
                "sourceRefs", parseSourceRefs(message.getSourceRefs())
        );
    }

    private KnowledgeBase requireBase(Long kbId) {
        KnowledgeBase base = knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> new IllegalArgumentException("知识库不存在: " + kbId));
        if (!currentUserService.requireCurrentUserId().equals(base.getOwnerUserId())) {
            throw new IllegalArgumentException("无权访问该知识库");
        }
        return base;
    }

    private AiChatSession requireSession(Long sessionId) {
        AiChatSession session = aiChatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在: " + sessionId));
        if (!currentUserService.requireCurrentUserId().equals(session.getUserId())) {
            throw new IllegalArgumentException("无权访问该会话");
        }
        return session;
    }

    private Long requireWorkspaceId() {
        Workspace workspace = workspaceRepository.findByOwnerUserIdOrderByUpdatedAtDesc(currentUserService.requireCurrentUserId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("当前用户尚未初始化工作区"));
        return workspace.getId();
    }

    private Long extractKbId(AiChatSession session) {
        String marker = kbMarkerPrefix();
        String sourceDocIds = session.getSourceDocIds();
        if (sourceDocIds != null && sourceDocIds.contains(marker)) {
            String number = sourceDocIds.replaceAll("[^0-9]", " ").trim().split("\\s+")[0];
            return Long.parseLong(number);
        }
        throw new IllegalStateException("该会话未绑定知识库");
    }

    private String kbMarker(Long kbId) {
        return kbMarkerPrefix() + kbId;
    }

    private String kbMarkerPrefix() {
        return "KB:";
    }

    private String autoTitle(String text) {
        String clean = text.replaceAll("\\s+", " ").trim();
        if (clean.length() > 18) {
            clean = clean.substring(0, 18);
        }
        return "知识库会话 · " + clean;
    }

    private Map<String, Object> toSessionDto(AiChatSession session) {
        List<AiChatMessage> messages = aiChatMessageRepository.findTop20BySessionIdOrderByCreatedAtAsc(session.getId());
        String preview = messages.isEmpty() ? "" : messages.get(messages.size() - 1).getContentText();
        return Map.of(
                "id", session.getId(),
                "title", session.getTitle(),
                "status", session.getStatus(),
                "updatedAt", session.getUpdatedAt() == null ? "" : session.getUpdatedAt().format(DATE_TIME_FORMATTER),
                "preview", preview.length() > 80 ? preview.substring(0, 80) + "..." : preview
        );
    }

    private Map<String, Object> toMessageDto(AiChatMessage message) {
        return Map.of(
                "id", message.getId(),
                "role", "ASSISTANT".equalsIgnoreCase(message.getRoleCode()) ? "assistant" : "user",
                "content", message.getContentText(),
                "createdAt", message.getCreatedAt() == null ? "" : message.getCreatedAt().format(DATE_TIME_FORMATTER),
                "sourceRefs", parseSourceRefs(message.getSourceRefs())
        );
    }

    private List<Map<String, Object>> parseSourceRefs(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ignored) {
            return List.of();
        }
    }
}
