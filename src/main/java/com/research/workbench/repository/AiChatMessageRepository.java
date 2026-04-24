package com.research.workbench.repository;

import com.research.workbench.domain.AiChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {

    List<AiChatMessage> findTop20BySessionIdOrderByCreatedAtAsc(Long sessionId);

    List<AiChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
}
