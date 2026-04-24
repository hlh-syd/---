package com.research.workbench.repository;

import com.research.workbench.domain.WorkspaceArtifact;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceArtifactRepository extends JpaRepository<WorkspaceArtifact, Long> {

    long countByCreatorUserId(Long creatorUserId);

    List<WorkspaceArtifact> findTop20ByCreatorUserIdOrderByUpdatedAtDesc(Long creatorUserId);
}
