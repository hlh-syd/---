package com.research.workbench.controller;

import com.research.workbench.service.KnowledgeBaseService;
import com.research.workbench.service.KnowledgeConversationService;
import com.research.workbench.service.KnowledgeRagService;
import com.research.workbench.controller.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeRagService knowledgeRagService;
    private final KnowledgeConversationService knowledgeConversationService;

    public KnowledgeBaseController(
            KnowledgeBaseService knowledgeBaseService,
            KnowledgeRagService knowledgeRagService,
            KnowledgeConversationService knowledgeConversationService
    ) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.knowledgeRagService = knowledgeRagService;
        this.knowledgeConversationService = knowledgeConversationService;
    }

    @GetMapping("/bases")
    public ApiResponse<List<Map<String, Object>>> listBases() {
        return ApiResponse.ok(knowledgeBaseService.listBases());
    }

    @PostMapping("/bases")
    public ApiResponse<Map<String, Object>> createBase(@RequestBody CreateBaseRequest request) {
        return ApiResponse.ok(knowledgeBaseService.createBase(request.name(), request.description()));
    }

    @PutMapping("/bases/{id}")
    public ApiResponse<Map<String, Object>> updateBase(@PathVariable Long id, @RequestBody CreateBaseRequest request) {
        return ApiResponse.ok(knowledgeBaseService.updateBase(id, request.name(), request.description()));
    }

    @DeleteMapping("/bases/{id}")
    public ApiResponse<Void> deleteBase(@PathVariable Long id) {
        knowledgeBaseService.deleteBase(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/bases/{id}/documents")
    public ApiResponse<List<Map<String, Object>>> listDocuments(@PathVariable Long id) {
        return ApiResponse.ok(knowledgeBaseService.listDocuments(id));
    }

    @GetMapping("/bases/{id}/documents/search")
    public ApiResponse<List<Map<String, Object>>> searchDocuments(@PathVariable Long id, @RequestParam("q") String q) {
        return ApiResponse.ok(knowledgeBaseService.searchDocuments(id, q));
    }

    @GetMapping("/bases/{id}/summary")
    public ApiResponse<Map<String, Object>> summary(@PathVariable Long id) {
        return ApiResponse.ok(knowledgeBaseService.summary(id));
    }

    @PostMapping("/bases/{id}/documents")
    public ApiResponse<Map<String, Object>> createTextDocument(@PathVariable Long id, @RequestBody CreateDocumentRequest request) {
        return ApiResponse.ok(knowledgeBaseService.createTextDocument(id, request.title(), request.content(), request.sourceType()));
    }

    @PostMapping("/bases/{id}/documents/upload")
    public ApiResponse<Map<String, Object>> uploadDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sourceType", required = false) String sourceType
    ) {
        return ApiResponse.ok(knowledgeBaseService.uploadDocument(id, file, sourceType));
    }

    @PutMapping("/documents/{id}")
    public ApiResponse<Map<String, Object>> updateDocument(@PathVariable Long id, @RequestBody UpdateDocumentRequest request) {
        return ApiResponse.ok(knowledgeBaseService.updateDocument(id, request.title(), request.summary(), request.sourceType()));
    }

    @DeleteMapping("/documents/{id}")
    public ApiResponse<Void> deleteDocument(@PathVariable Long id) {
        knowledgeBaseService.deleteDocument(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/documents/batch-delete")
    public ApiResponse<Void> batchDelete(@RequestBody BatchDeleteRequest request) {
        knowledgeBaseService.batchDeleteDocuments(request.documentIds());
        return ApiResponse.ok(null);
    }

    @PostMapping("/documents/{id}/parse")
    public ApiResponse<Map<String, Object>> parseDocument(@PathVariable Long id) {
        return ApiResponse.ok(knowledgeRagService.parseDocument(id));
    }

    @PostMapping("/bases/{id}/parse")
    public ApiResponse<List<Map<String, Object>>> parseBase(@PathVariable Long id) {
        return ApiResponse.ok(knowledgeRagService.parseBase(id));
    }

    @GetMapping("/documents/{id}/citations")
    public ApiResponse<List<Map<String, Object>>> listCitations(@PathVariable Long id) {
        return ApiResponse.ok(knowledgeRagService.listCitations(id));
    }

    @PostMapping("/bases/{id}/qa")
    public ApiResponse<Map<String, Object>> answerQuestion(@PathVariable Long id, @RequestBody QaRequest request) {
        return ApiResponse.ok(knowledgeRagService.answerQuestion(id, request.question()));
    }

    @GetMapping("/bases/{id}/sessions")
    public ApiResponse<List<Map<String, Object>>> listSessions(@PathVariable Long id) {
        return ApiResponse.ok(knowledgeConversationService.listSessions(id));
    }

    @PostMapping("/bases/{id}/sessions")
    public ApiResponse<Map<String, Object>> createSession(@PathVariable Long id, @RequestBody CreateSessionRequest request) {
        return ApiResponse.ok(knowledgeConversationService.createSession(id, request.title()));
    }

    @GetMapping("/sessions/{id}/messages")
    public ApiResponse<List<Map<String, Object>>> listMessages(@PathVariable Long id) {
        return ApiResponse.ok(knowledgeConversationService.listMessages(id));
    }

    @PostMapping("/sessions/{id}/messages")
    public ApiResponse<Map<String, Object>> askInSession(@PathVariable Long id, @RequestBody QaRequest request) {
        return ApiResponse.ok(knowledgeConversationService.ask(id, request.question()));
    }

    @PutMapping("/sessions/{id}")
    public ApiResponse<Map<String, Object>> renameSession(@PathVariable Long id, @RequestBody CreateSessionRequest request) {
        return ApiResponse.ok(knowledgeConversationService.renameSession(id, request.title()));
    }

    @DeleteMapping("/sessions/{id}")
    public ApiResponse<Void> deleteSession(@PathVariable Long id) {
        knowledgeConversationService.deleteSession(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/sessions/{id}/auto-title")
    public ApiResponse<Map<String, Object>> autoTitleSession(@PathVariable Long id) {
        return ApiResponse.ok(knowledgeConversationService.autoTitleSession(id));
    }

    @PostMapping("/sessions/{sessionId}/messages/{messageId}/save")
    public ApiResponse<Map<String, Object>> saveAnswer(
            @PathVariable Long sessionId,
            @PathVariable Long messageId,
            @RequestBody SaveAnswerRequest request
    ) {
        return ApiResponse.ok(knowledgeConversationService.saveAssistantMessageToKnowledge(sessionId, messageId, request.title()));
    }

    public record CreateBaseRequest(@NotBlank String name, String description) {
    }

    public record CreateDocumentRequest(@NotBlank String title, String content, String sourceType) {
    }

    public record UpdateDocumentRequest(@NotBlank String title, String summary, String sourceType) {
    }

    public record QaRequest(@NotBlank String question) {
    }

    public record CreateSessionRequest(String title) {
    }

    public record SaveAnswerRequest(String title) {
    }

    public record BatchDeleteRequest(List<Long> documentIds) {
    }
}
