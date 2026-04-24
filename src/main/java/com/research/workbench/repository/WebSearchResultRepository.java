package com.research.workbench.repository;

import com.research.workbench.domain.WebSearchResult;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebSearchResultRepository extends JpaRepository<WebSearchResult, Long> {

    List<WebSearchResult> findDistinctByTitleContainingIgnoreCaseOrSnippetTextContainingIgnoreCaseOrderByCreatedAtDesc(
            String titleKeyword,
            String snippetKeyword
    );

    List<WebSearchResult> findTop20ByOrderByCreatedAtDesc();

    List<WebSearchResult> findByTaskIdOrderByCreatedAtAsc(Long taskId);
}
