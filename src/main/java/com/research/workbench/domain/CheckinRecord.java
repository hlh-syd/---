package com.research.workbench.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "checkin_record")
public class CheckinRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "checkin_date", nullable = false)
    private LocalDate checkinDate;

    @Column(name = "focus_minutes", nullable = false)
    private Integer focusMinutes = 0;

    @Column(name = "completed_task_count", nullable = false)
    private Integer completedTaskCount = 0;

    @Column(name = "summary_text", length = 500)
    private String summaryText;

    @Column(name = "mood_code", length = 16)
    private String moodCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
