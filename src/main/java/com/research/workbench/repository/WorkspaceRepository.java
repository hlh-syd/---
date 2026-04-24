package com.research.workbench.repository;

import com.research.workbench.domain.Workspace;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    List<Workspace> findByOwnerUserIdOrderByUpdatedAtDesc(Long ownerUserId);
}
