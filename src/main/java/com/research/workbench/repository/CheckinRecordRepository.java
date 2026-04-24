package com.research.workbench.repository;

import com.research.workbench.domain.CheckinRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckinRecordRepository extends JpaRepository<CheckinRecord, Long> {

    List<CheckinRecord> findTop20ByUserIdOrderByCheckinDateDesc(Long userId);
}
