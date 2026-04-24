package com.research.workbench.repository;

import com.research.workbench.domain.PomodoroRecord;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PomodoroRecordRepository extends JpaRepository<PomodoroRecord, Long> {

    List<PomodoroRecord> findTop20ByUserIdOrderByStartedAtDesc(Long userId);

    List<PomodoroRecord> findByUserIdAndStartedAtGreaterThanEqualOrderByStartedAtAsc(Long userId, LocalDateTime startedAt);
}
