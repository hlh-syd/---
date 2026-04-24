package com.research.workbench.repository;

import com.research.workbench.domain.KnowledgeChunk;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {

    List<KnowledgeChunk> findByDocumentIdOrderByChunkNoAsc(Long documentId);

    void deleteByDocumentId(Long documentId);
}
