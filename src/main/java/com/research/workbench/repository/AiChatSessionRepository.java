package com.research.workbench.repository;

import com.research.workbench.domain.AiChatSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiChatSessionRepository extends JpaRepository<AiChatSession, Long> {

    List<AiChatSession> findTop10ByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<AiChatSession> findTopByUserIdOrderByUpdatedAtDesc(Long userId);

    List<AiChatSession> findByUserIdAndAssistantTypeAndSourceDocIdsContainingOrderByUpdatedAtDesc(
            Long userId,
            String assistantType,
            String sourceDocIds
    );
}
