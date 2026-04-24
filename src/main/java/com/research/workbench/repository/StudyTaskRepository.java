package com.research.workbench.repository;

import com.research.workbench.domain.StudyTask;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyTaskRepository extends JpaRepository<StudyTask, Long> {

    List<StudyTask> findTop20ByUserIdOrderByDueTimeAsc(Long userId);

    long countByUserIdAndTaskStatus(Long userId, String taskStatus);
}
