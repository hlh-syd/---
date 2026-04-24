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
@Table(name = "pomodoro_session_log")
public class PomodoroSessionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "session_time", nullable = false)
    private LocalDateTime sessionTime;

    @Column(name = "session_count", nullable = false)
    private Integer sessionCount;

    @Column(name = "daily_sequence", nullable = false)
    private Integer dailySequence;

    @Column(name = "focus_minutes")
    private Integer focusMinutes;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
