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
@Table(name = "pomodoro_record")
public class PomodoroRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "focus_minutes", nullable = false)
    private Integer focusMinutes = 25;

    @Column(name = "break_minutes", nullable = false)
    private Integer breakMinutes = 5;

    @Column(name = "cycle_index", nullable = false)
    private Integer cycleIndex = 1;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(nullable = false, length = 16)
    private String status = "DONE";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
