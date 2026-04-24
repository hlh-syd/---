package com.research.workbench.repository;

import com.research.workbench.domain.UserSocialBinding;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSocialBindingRepository extends JpaRepository<UserSocialBinding, Long> {

    List<UserSocialBinding> findByUserIdOrderByPlatformAsc(Long userId);
}
