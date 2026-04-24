package com.research.workbench.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "web_search_result")
public class WebSearchResult extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "platform_name", nullable = false, length = 64)
    private String platformName;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "author_name", length = 128)
    private String authorName;

    @Column(name = "publish_at")
    private LocalDateTime publishAt;

    @Column(length = 500)
    private String url;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(name = "snippet_text", columnDefinition = "TEXT")
    private String snippetText;

    @Column(name = "markdown_content", columnDefinition = "MEDIUMTEXT")
    private String markdownContent;

    private BigDecimal score;

    @Column(name = "raw_json", columnDefinition = "json")
    private String rawJson;
}
