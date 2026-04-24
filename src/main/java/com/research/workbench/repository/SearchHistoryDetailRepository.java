package com.research.workbench.repository;

import com.research.workbench.domain.SearchHistoryDetail;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchHistoryDetailRepository extends JpaRepository<SearchHistoryDetail, Long> {

    long countBySessionId(String sessionId);

    List<SearchHistoryDetail> findBySessionIdOrderByDetailNoAsc(String sessionId);
}
