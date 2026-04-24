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
@Table(name = "search_history_detail")
public class SearchHistoryDetail extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_name", nullable = false, length = 64)
    private String userName;

    @Column(name = "detail_no", nullable = false)
    private Integer detailNo;

    @Column(name = "query_text", columnDefinition = "LONGTEXT")
    private String queryText;

    @Column(name = "answer_text", columnDefinition = "LONGTEXT")
    private String answerText;

    @Column(name = "extra_json", columnDefinition = "json")
    private String extraJson;
}
