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
@Table(name = "workspace_artifact")
public class WorkspaceArtifact extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "creator_user_id", nullable = false)
    private Long creatorUserId;

    @Column(name = "artifact_type", nullable = false, length = 32)
    private String artifactType;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;

    @Column(name = "source_refs", columnDefinition = "json")
    private String sourceRefs;

    @Column(name = "content_markdown", columnDefinition = "LONGTEXT")
    private String contentMarkdown;

    @Column(name = "extra_json", columnDefinition = "json")
    private String extraJson;

    @Column(nullable = false, length = 16)
    private String status = "READY";
}
