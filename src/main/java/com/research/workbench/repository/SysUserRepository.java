package com.research.workbench.repository;

import com.research.workbench.domain.SysUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SysUserRepository extends JpaRepository<SysUser, Long> {

    Optional<SysUser> findByUsername(String username);

    Optional<SysUser> findByUsernameIgnoreCase(String username);

    Optional<SysUser> findByEmailIgnoreCase(String email);

    Optional<SysUser> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);
}
