package com.research.workbench.repository;

import com.research.workbench.domain.PaperSearchResult;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperSearchResultRepository extends JpaRepository<PaperSearchResult, Long> {

    List<PaperSearchResult> findTop20ByOrderByCreatedAtDesc();

    List<PaperSearchResult> findByTaskIdOrderByCreatedAtAsc(Long taskId);

    List<PaperSearchResult> findDistinctByTitleContainingIgnoreCaseOrAbstractTextContainingIgnoreCaseOrderByPublishYearDesc(
            String titleKeyword,
            String abstractKeyword
    );
}
