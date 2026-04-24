package com.research.workbench.service;

import com.research.workbench.service.UserInitializationService;
import com.research.workbench.domain.SysUser;
import com.research.workbench.repository.SysUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AuthService {

    private final SysUserRepository sysUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final CurrentUserService currentUserService;
    private final UserInitializationService userInitializationService;

    public AuthService(
            SysUserRepository sysUserRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            CurrentUserService currentUserService,
            UserInitializationService userInitializationService
    ) {
        this.sysUserRepository = sysUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.currentUserService = currentUserService;
        this.userInitializationService = userInitializationService;
    }

    public Map<String, Object> login(String identifier, String password, HttpServletRequest request, HttpServletResponse response) {
        if (!StringUtils.hasText(identifier) || !StringUtils.hasText(password)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名和密码不能为空");
        }
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(identifier.trim(), password)
            );
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);

            SysUser user = sysUserRepository.findByUsernameIgnoreCase(authentication.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录用户不存在"));
            user.setLastLoginAt(LocalDateTime.now());
            userInitializationService.ensureInitialized(user);
            return authPayload(sysUserRepository.save(user));
        } catch (BadCredentialsException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
    }

    public Map<String, Object> register(String username, String email, String nickname, String password, HttpServletRequest request, HttpServletResponse response) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password) || !StringUtils.hasText(nickname)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名、昵称和密码不能为空");
        }
        String normalizedUsername = username.trim();
        String normalizedEmail = email == null ? "" : email.trim();
        if (normalizedUsername.length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名至少需要 3 个字符");
        }
        if (password.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码至少需要 6 个字符");
        }
        if (sysUserRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
        }
        if (StringUtils.hasText(normalizedEmail) && sysUserRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "邮箱已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(normalizedUsername);
        user.setEmail(StringUtils.hasText(normalizedEmail) ? normalizedEmail : null);
        user.setNickname(nickname.trim());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRoleCode("USER");
        user.setStatus(1);
        user.setLastLoginAt(LocalDateTime.now());
        user = sysUserRepository.save(user);
        userInitializationService.ensureInitialized(user);

        return login(normalizedUsername, password, request, response);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> me() {
        return authPayload(currentUserService.requireCurrentUser());
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, authentication);
    }

    private Map<String, Object> authPayload(SysUser user) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", user.getId());
        payload.put("username", user.getUsername());
        payload.put("email", user.getEmail() == null ? "" : user.getEmail());
        payload.put("nickname", user.getNickname());
        payload.put("role", user.getRoleCode());
        return payload;
    }
}
