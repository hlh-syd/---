package com.research.workbench.repository;

import com.research.workbench.domain.ResearchProject;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResearchProjectRepository extends JpaRepository<ResearchProject, Long> {

    List<ResearchProject> findByWorkspaceIdOrderByUpdatedAtDesc(Long workspaceId);
}
