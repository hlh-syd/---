package com.research.workbench.repository;

import com.research.workbench.domain.KnowledgeBase;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {

    List<KnowledgeBase> findByOwnerUserIdOrderByUpdatedAtDesc(Long ownerUserId);
}
