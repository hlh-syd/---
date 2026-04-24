package com.research.workbench.repository;

import com.research.workbench.domain.PomodoroSessionLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PomodoroSessionLogRepository extends JpaRepository<PomodoroSessionLog, Long> {

    List<PomodoroSessionLog> findTop20ByUserIdOrderBySessionTimeDesc(Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndSessionTimeBetween(Long userId, LocalDateTime start, LocalDateTime end);
}
