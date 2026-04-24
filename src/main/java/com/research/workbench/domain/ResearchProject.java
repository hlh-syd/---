package com.research.workbench.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "research_project")
public class ResearchProject extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 255)
    private String topic;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "color_token", length = 32)
    private String colorToken;

    @Column(name = "stage_code", nullable = false, length = 32)
    private String stageCode = "IDEA";

    @Column(name = "due_date")
    private LocalDate dueDate;
}
