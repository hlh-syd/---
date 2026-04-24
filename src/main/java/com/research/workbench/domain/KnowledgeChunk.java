package com.research.workbench.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "knowledge_chunk")
public class KnowledgeChunk extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "chunk_no", nullable = false)
    private Integer chunkNo;

    @Column(name = "content_text", nullable = false, columnDefinition = "LONGTEXT")
    private String contentText;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "keyword_json", columnDefinition = "json")
    private String keywordJson;

    @Column(name = "embedding_status", nullable = false, length = 16)
    private String embeddingStatus = "NONE";
}
