package com.research.workbench.repository;

import com.research.workbench.domain.PaperSearchTask;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperSearchTaskRepository extends JpaRepository<PaperSearchTask, Long> {

    List<PaperSearchTask> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
}
