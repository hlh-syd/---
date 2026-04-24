package com.research.workbench.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "study_task")
public class StudyTask extends BaseAuditEntity {

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

    @Column(length = 1000)
    private String description;

    @Column(name = "task_status", nullable = false, length = 16)
    private String taskStatus = "TODO";

    @Column(name = "priority_level", nullable = false)
    private Integer priorityLevel = 2;

    @Column(name = "source_type", length = 32)
    private String sourceType;

    @Column(name = "source_ref_id")
    private Long sourceRefId;

    @Column(name = "due_time")
    private LocalDateTime dueTime;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
}
