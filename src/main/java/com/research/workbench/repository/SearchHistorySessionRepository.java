package com.research.workbench.repository;

import com.research.workbench.domain.SearchHistorySession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchHistorySessionRepository extends JpaRepository<SearchHistorySession, Long> {

    Optional<SearchHistorySession> findBySessionId(String sessionId);
}
