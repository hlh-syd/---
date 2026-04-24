package com.research.workbench.service;

import com.research.workbench.service.CurrentUserService;
import com.research.workbench.config.AppProperties;
import com.research.workbench.domain.KnowledgeBase;
import com.research.workbench.domain.KnowledgeDocument;
import com.research.workbench.domain.Workspace;
import com.research.workbench.integration.LlmChatClient;
import com.research.workbench.repository.KnowledgeBaseRepository;
import com.research.workbench.repository.KnowledgeChunkRepository;
import com.research.workbench.repository.KnowledgeDocumentRepository;
import com.research.workbench.repository.WorkspaceRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class KnowledgeBaseService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeRagService knowledgeRagService;
    private final AppProperties appProperties;
    private final LlmChatClient llmChatClient;
    private final CurrentUserService currentUserService;
    private final WorkspaceRepository workspaceRepository;

    public KnowledgeBaseService(
            KnowledgeBaseRepository knowledgeBaseRepository,
            KnowledgeDocumentRepository knowledgeDocumentRepository,
            KnowledgeChunkRepository knowledgeChunkRepository,
            KnowledgeRagService knowledgeRagService,
            AppProperties appProperties,
            LlmChatClient llmChatClient,
            CurrentUserService currentUserService,
            WorkspaceRepository workspaceRepository
    ) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.knowledgeRagService = knowledgeRagService;
        this.appProperties = appProperties;
        this.llmChatClient = llmChatClient;
        this.currentUserService = currentUserService;
        this.workspaceRepository = workspaceRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listBases() {
        return knowledgeBaseRepository.findByOwnerUserIdOrderByUpdatedAtDesc(currentUserService.requireCurrentUserId())
                .stream()
                .map(this::toBaseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listDocuments(Long kbId) {
        KnowledgeBase base = requireBase(kbId);
        return knowledgeDocumentRepository.findTop20ByKbIdOrderByUpdatedAtDesc(base.getId())
                .stream()
                .sorted(Comparator.comparing(KnowledgeDocument::getUpdatedAt).reversed())
                .map(this::toDocumentDto)
                .toList();
    }

    public Map<String, Object> createBase(String name, String description) {
        KnowledgeBase base = new KnowledgeBase();
        base.setWorkspaceId(requireWorkspaceId());
        base.setOwnerUserId(currentUserService.requireCurrentUserId());
        base.setName(name);
        base.setDescription(description);
        base = knowledgeBaseRepository.save(base);
        return toBaseDto(base);
    }

    public Map<String, Object> updateBase(Long id, String name, String description) {
        KnowledgeBase base = requireBase(id);
        base.setName(name);
        base.setDescription(description);
        return toBaseDto(knowledgeBaseRepository.save(base));
    }

    public void deleteBase(Long id) {
        KnowledgeBase base = requireBase(id);
        List<KnowledgeDocument> documents = knowledgeDocumentRepository.findByKbIdOrderByUpdatedAtDesc(base.getId());
        documents.forEach(this::deleteDocumentInternal);
        knowledgeBaseRepository.delete(base);
    }

    public Map<String, Object> createTextDocument(Long kbId, String title, String content, String sourceType) {
        KnowledgeBase base = requireBase(kbId);
        KnowledgeDocument document = new KnowledgeDocument();
        document.setKbId(base.getId());
        document.setUploadedBy(currentUserService.requireCurrentUserId());
        document.setSourceType(sourceType == null || sourceType.isBlank() ? "TEXT" : sourceType);
        document.setTitle(title);
        document.setFileName(title + ".md");
        document.setFileExt("md");
        document.setParseStatus("READY");
        document.setChunkCount(estimateChunks(content));
        document.setSummaryText(content);
        document.setTagJson("[\"TEXT\",\"MANUAL\"]");
        document = knowledgeDocumentRepository.save(document);
        knowledgeRagService.parseDocument(document.getId());
        refreshBaseStats(base.getId());
        return toDocumentDto(document);
    }

    public Map<String, Object> updateDocument(Long documentId, String title, String summary, String sourceType) {
        KnowledgeDocument document = requireDocument(documentId);
        document.setTitle(title);
        document.setSummaryText(summary);
        if (StringUtils.hasText(sourceType)) {
            document.setSourceType(sourceType);
        }
        return toDocumentDto(knowledgeDocumentRepository.save(document));
    }

    public Map<String, Object> uploadDocument(Long kbId, MultipartFile file, String sourceType) {
        KnowledgeBase base = requireBase(kbId);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = StringUtils.getFilenameExtension(originalFilename);
        Path targetDir = Path.of(appProperties.getFile().getUploadDir(), "knowledge", String.valueOf(kbId));
        try {
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(System.currentTimeMillis() + "_" + originalFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            KnowledgeDocument document = new KnowledgeDocument();
            document.setKbId(base.getId());
            document.setUploadedBy(currentUserService.requireCurrentUserId());
            document.setSourceType(StringUtils.hasText(sourceType) ? sourceType : "UPLOAD");
            document.setTitle(originalFilename);
            document.setFileName(originalFilename);
            document.setFileExt(extension == null ? "" : extension);
            document.setFileSize(file.getSize());
            document.setStoragePath(targetPath.toString());
            document.setParseStatus("UPLOADED");
            document.setChunkCount(0);
            document.setSummaryText("文件已上传，可继续解析、问答和引用回溯。");
            document.setTagJson("[\"UPLOAD\"]");
            document = knowledgeDocumentRepository.save(document);
            knowledgeRagService.parseDocument(document.getId());
            refreshBaseStats(base.getId());
            return toDocumentDto(document);
        } catch (IOException e) {
            throw new IllegalStateException("文件上传失败: " + e.getMessage(), e);
        }
    }

    public void deleteDocument(Long documentId) {
        KnowledgeDocument document = requireDocument(documentId);
        Long kbId = document.getKbId();
        deleteDocumentInternal(document);
        refreshBaseStats(kbId);
    }

    public void batchDeleteDocuments(List<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return;
        }
        Long kbId = null;
        for (Long documentId : documentIds) {
            KnowledgeDocument document = requireDocument(documentId);
            kbId = document.getKbId();
            deleteDocumentInternal(document);
        }
        if (kbId != null) {
            refreshBaseStats(kbId);
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchDocuments(Long kbId, String keyword) {
        KnowledgeBase base = requireBase(kbId);
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
        return knowledgeDocumentRepository.findByKbIdOrderByUpdatedAtDesc(base.getId())
                .stream()
                .filter(document -> normalized.isBlank()
                        || (document.getTitle() != null && document.getTitle().toLowerCase().contains(normalized))
                        || (document.getSummaryText() != null && document.getSummaryText().toLowerCase().contains(normalized)))
                .map(this::toDocumentDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary(Long kbId) {
        KnowledgeBase base = requireBase(kbId);
        List<KnowledgeDocument> documents = knowledgeDocumentRepository.findByKbIdOrderByUpdatedAtDesc(base.getId());
        String combined = documents.stream()
                .map(document -> document.getTitle() + "：" + (document.getSummaryText() == null ? "" : document.getSummaryText()))
                .limit(8)
                .reduce("", (a, b) -> a + "\n" + b)
                .trim();
        String answer;
        try {
            answer = llmChatClient.chat(
                    "你是知识库总结助手，请输出中文摘要、关键主题和后续建议。",
                    combined
            );
        } catch (Exception ignored) {
            answer = "知识库总结：当前资料主要围绕研究工作流、知识沉淀与引用回溯，建议优先整理高频主题并补充结构化标签。";
        }
        return Map.of("summary", answer, "documentCount", documents.size());
    }

    private void deleteDocumentInternal(KnowledgeDocument document) {
        knowledgeChunkRepository.deleteByDocumentId(document.getId());
        if (StringUtils.hasText(document.getStoragePath())) {
            try {
                Files.deleteIfExists(Path.of(document.getStoragePath()));
            } catch (IOException ignored) {
            }
        }
        knowledgeDocumentRepository.delete(document);
    }

    private void refreshBaseStats(Long kbId) {
        KnowledgeBase base = requireBase(kbId);
        List<KnowledgeDocument> documents = knowledgeDocumentRepository.findByKbIdOrderByUpdatedAtDesc(kbId);
        base.setDocCount(documents.size());
        base.setTotalChunkCount(documents.stream().mapToInt(document -> document.getChunkCount() == null ? 0 : document.getChunkCount()).sum());
        knowledgeBaseRepository.save(base);
    }

    private int estimateChunks(String content) {
        if (!StringUtils.hasText(content)) {
            return 0;
        }
        return Math.max(1, content.length() / 800);
    }

    private KnowledgeBase requireBase(Long id) {
        KnowledgeBase base = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("知识库不存在: " + id));
        if (!currentUserService.requireCurrentUserId().equals(base.getOwnerUserId())) {
            throw new IllegalArgumentException("无权访问该知识库");
        }
        return base;
    }

    private KnowledgeDocument requireDocument(Long id) {
        KnowledgeDocument document = knowledgeDocumentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在: " + id));
        requireBase(document.getKbId());
        return document;
    }

    private Long requireWorkspaceId() {
        Workspace workspace = workspaceRepository.findByOwnerUserIdOrderByUpdatedAtDesc(currentUserService.requireCurrentUserId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("当前用户尚未初始化工作区"));
        return workspace.getId();
    }

    private Map<String, Object> toBaseDto(KnowledgeBase base) {
        return Map.of(
                "id", base.getId(),
                "name", base.getName(),
                "description", base.getDescription() == null ? "" : base.getDescription(),
                "visibility", base.getVisibility(),
                "docCount", base.getDocCount(),
                "chunkCount", base.getTotalChunkCount(),
                "updatedAt", base.getUpdatedAt() == null ? "" : base.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "meta", base.getDocCount() + " 份资料 / " + base.getTotalChunkCount() + " 段切片"
        );
    }

    private Map<String, Object> toDocumentDto(KnowledgeDocument document) {
        return Map.of(
                "id", document.getId(),
                "kbId", document.getKbId(),
                "title", document.getTitle(),
                "source", document.getSourceType(),
                "status", document.getParseStatus(),
                "chunkCount", document.getChunkCount() == null ? 0 : document.getChunkCount(),
                "date", document.getUpdatedAt() == null ? "" : document.getUpdatedAt().toLocalDate().format(DATE_FORMATTER),
                "summary", document.getSummaryText() == null ? "" : document.getSummaryText(),
                "fileName", document.getFileName() == null ? "" : document.getFileName()
        );
    }
}
