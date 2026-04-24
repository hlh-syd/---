package com.research.workbench.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "paper_search_result")
public class PaperSearchResult extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "source_name", nullable = false, length = 64)
    private String sourceName;

    @Column(name = "external_id", length = 128)
    private String externalId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 500)
    private String authors;

    @Column(name = "abstract_text", columnDefinition = "LONGTEXT")
    private String abstractText;

    @Column(name = "publish_year")
    private Integer publishYear;

    @Column(name = "journal_name", length = 255)
    private String journalName;

    @Column(length = 128)
    private String doi;

    @Column(name = "paper_url", length = 500)
    private String paperUrl;

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    @Column(name = "citation_count")
    private Integer citationCount;

    @Column(name = "keyword_json", columnDefinition = "json")
    private String keywordJson;

    private BigDecimal score;

    @Column(name = "raw_json", columnDefinition = "json")
    private String rawJson;
}
