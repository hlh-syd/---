package com.research.workbench.repository;

import com.research.workbench.domain.KnowledgeFolder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeFolderRepository extends JpaRepository<KnowledgeFolder, Long> {

    List<KnowledgeFolder> findByKbIdOrderBySortNoAscUpdatedAtDesc(Long kbId);
}
