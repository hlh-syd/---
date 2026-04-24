package com.research.workbench.repository;

import com.research.workbench.domain.UserDailyUsage;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDailyUsageRepository extends JpaRepository<UserDailyUsage, Long> {

    Optional<UserDailyUsage> findByUserIdAndUsageDate(Long userId, LocalDate usageDate);
}
