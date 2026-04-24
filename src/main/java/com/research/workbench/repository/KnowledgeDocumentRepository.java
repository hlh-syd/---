package com.research.workbench.repository;

import com.research.workbench.domain.KnowledgeDocument;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {

    List<KnowledgeDocument> findTop20ByKbIdOrderByUpdatedAtDesc(Long kbId);

    List<KnowledgeDocument> findByKbIdOrderByUpdatedAtDesc(Long kbId);

    List<KnowledgeDocument> findTop20ByUploadedByOrderByUpdatedAtDesc(Long uploadedBy);
}
