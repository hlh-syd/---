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
@Table(name = "web_search_task")
public class WebSearchTask extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "workspace_id")
    private Long workspaceId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "query_text", nullable = false, length = 500)
    private String queryText;

    @Column(name = "platform_scope", nullable = false, length = 128)
    private String platformScope = "webpage";

    @Column(name = "filters_json", columnDefinition = "json")
    private String filtersJson;

    @Column(nullable = false, length = 16)
    private String status = "SUCCESS";

    @Column(name = "result_count", nullable = false)
    private Integer resultCount = 0;

    @Column(name = "markdown_summary", columnDefinition = "MEDIUMTEXT")
    private String markdownSummary;
}
