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
@Table(name = "knowledge_document")
public class KnowledgeDocument extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kb_id", nullable = false)
    private Long kbId;

    @Column(name = "folder_id")
    private Long folderId;

    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType = "UPLOAD";

    @Column(name = "source_ref_id")
    private Long sourceRefId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_ext", length = 16)
    private String fileExt;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "storage_path", length = 500)
    private String storagePath;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "summary_text", columnDefinition = "TEXT")
    private String summaryText;

    @Column(name = "tag_json", columnDefinition = "json")
    private String tagJson;

    @Column(name = "parse_status", nullable = false, length = 16)
    private String parseStatus = "PENDING";

    @Column(name = "chunk_count", nullable = false)
    private Integer chunkCount = 0;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;
}
