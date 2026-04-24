package com.research.workbench.repository;

import com.research.workbench.domain.CalendarEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    List<CalendarEvent> findTop10ByUserIdAndStartTimeGreaterThanEqualOrderByStartTimeAsc(Long userId, LocalDateTime startTime);
}
