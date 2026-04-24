package com.research.workbench.repository;

import com.research.workbench.domain.WebSearchTask;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebSearchTaskRepository extends JpaRepository<WebSearchTask, Long> {

    List<WebSearchTask> findTop10ByUserIdOrderByUpdatedAtDesc(Long userId);
}
