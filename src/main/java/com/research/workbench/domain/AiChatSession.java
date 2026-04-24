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
@Table(name = "ai_chat_session")
public class AiChatSession extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "workspace_id")
    private Long workspaceId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "assistant_type", nullable = false, length = 32)
    private String assistantType;

    @Column(name = "source_doc_ids", columnDefinition = "json")
    private String sourceDocIds;

    @Column(nullable = false, length = 16)
    private String status = "ACTIVE";
}
