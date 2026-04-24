package com.research.workbench.service;

import com.research.workbench.service.CurrentUserService;
import com.research.workbench.domain.SysUser;
import com.research.workbench.domain.UserProfile;
import com.research.workbench.domain.UserSocialBinding;
import com.research.workbench.config.AppProperties;
import com.research.workbench.repository.SysUserRepository;
import com.research.workbench.repository.UserProfileRepository;
import com.research.workbench.repository.UserSocialBindingRepository;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class ProfileFeatureService {

    private final UserProfileRepository userProfileRepository;
    private final UserSocialBindingRepository userSocialBindingRepository;
    private final SysUserRepository sysUserRepository;
    private final AppProperties appProperties;
    private final CurrentUserService currentUserService;

    public ProfileFeatureService(
            UserProfileRepository userProfileRepository,
            UserSocialBindingRepository userSocialBindingRepository,
            SysUserRepository sysUserRepository,
            AppProperties appProperties,
            CurrentUserService currentUserService
    ) {
        this.userProfileRepository = userProfileRepository;
        this.userSocialBindingRepository = userSocialBindingRepository;
        this.sysUserRepository = sysUserRepository;
        this.appProperties = appProperties;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> profile() {
        Long userId = Objects.requireNonNull(currentUserService.requireCurrentUserId());
        UserProfile profile = userProfileRepository.findByUserId(userId).orElseThrow();
        SysUser user = sysUserRepository.findById(userId).orElseThrow();
        return Map.of(
                "realName", nullToEmpty(profile.getRealName()),
                "displayName", firstNonBlank(profile.getRealName(), user.getNickname()),
                "bio", nullToEmpty(profile.getBio()),
                "institution", nullToEmpty(profile.getInstitution()),
                "department", nullToEmpty(profile.getDepartment()),
                "researchDirection", nullToEmpty(profile.getResearchDirection()),
                "degreeLevel", nullToEmpty(profile.getDegreeLevel()),
                "gender", normalizeGender(profile.getGender()),
                "avatarUrl", nullToEmpty(user.getAvatarUrl())
        );
    }

    public Map<String, Object> update(ProfileRequest request) {
        Long userId = Objects.requireNonNull(currentUserService.requireCurrentUserId());
        UserProfile profile = userProfileRepository.findByUserId(userId).orElseThrow();
        SysUser user = sysUserRepository.findById(userId).orElseThrow();
        profile.setRealName(mergeText(request.realName(), profile.getRealName()));
        profile.setBio(mergeText(request.bio(), profile.getBio()));
        profile.setInstitution(mergeText(request.institution(), profile.getInstitution()));
        profile.setDepartment(mergeText(request.department(), profile.getDepartment()));
        profile.setResearchDirection(mergeText(request.researchDirection(), profile.getResearchDirection()));
        profile.setDegreeLevel(mergeText(request.degreeLevel(), profile.getDegreeLevel()));
        profile.setGender(mergeGender(request.gender(), profile.getGender()));
        user.setAvatarUrl(mergeText(request.avatarUrl(), user.getAvatarUrl()));
        userProfileRepository.save(profile);
        sysUserRepository.save(user);
        return profile();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> bindings() {
        return userSocialBindingRepository.findByUserIdOrderByPlatformAsc(currentUserService.requireCurrentUserId())
                .stream()
                .map(binding -> Map.<String, Object>of(
                        "id", binding.getId(),
                        "platform", binding.getPlatform(),
                        "openId", binding.getOpenId()
                ))
                .toList();
    }

    public Map<String, Object> bind(String platform, String openId) {
        Long userId = currentUserService.requireCurrentUserId();
        UserSocialBinding binding = userSocialBindingRepository.findByUserIdOrderByPlatformAsc(userId)
                .stream()
                .filter(item -> platform.equalsIgnoreCase(item.getPlatform()))
                .findFirst()
                .orElseGet(UserSocialBinding::new);
        binding.setUserId(userId);
        binding.setPlatform(platform);
        binding.setOpenId(openId);
        if (binding.getBoundAt() == null) {
            binding.setBoundAt(java.time.LocalDateTime.now());
        }
        binding = userSocialBindingRepository.save(binding);
        return Map.of("id", binding.getId(), "platform", binding.getPlatform(), "openId", binding.getOpenId());
    }

    public Map<String, Object> uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("头像文件不能为空");
        }
        String normalizedContentType = String.valueOf(file.getContentType()).toLowerCase(Locale.ROOT);
        if (!normalizedContentType.startsWith("image/")) {
            throw new IllegalArgumentException("仅支持图片文件");
        }

        Long userId = Objects.requireNonNull(currentUserService.requireCurrentUserId());
        SysUser user = sysUserRepository.findById(userId).orElseThrow();
        String originalFilenameRaw = file.getOriginalFilename();
        String originalFilename = sanitizeFilename(originalFilenameRaw);
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String safeExt = StringUtils.hasText(extension) ? Objects.requireNonNull(extension).toLowerCase(Locale.ROOT) : "png";
        String storedName = System.currentTimeMillis() + "_" + userId + "." + safeExt;
        Path userAvatarDir = resolveUserAvatarDir(userId);
        Path targetPath = userAvatarDir.resolve(storedName).normalize();

        try {
            Files.createDirectories(userAvatarDir);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            String avatarUrl = "/api/profile/avatar/" + storedName;
            user.setAvatarUrl(avatarUrl);
            sysUserRepository.save(user);
            return Map.of("avatarUrl", avatarUrl, "fileName", storedName);
        } catch (IOException e) {
            throw new IllegalStateException("头像上传失败: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<AvatarResource> readAvatar(String filename) {
        Long userId = Objects.requireNonNull(currentUserService.requireCurrentUserId());
        SysUser user = sysUserRepository.findById(userId).orElseThrow();
        String currentAvatar = nullToEmpty(user.getAvatarUrl());
        if (currentAvatar.isBlank() || !currentAvatar.endsWith("/" + filename)) {
            return Optional.empty();
        }
        String safeFilename = sanitizeFilename(filename);
        if (!StringUtils.hasText(safeFilename) || safeFilename.contains("..")) {
            return Optional.empty();
        }

        Path avatarPath = resolveUserAvatarDir(userId).resolve(safeFilename).normalize();
        if (!avatarPath.startsWith(resolveUserAvatarDir(userId))) {
            return Optional.empty();
        }
        if (!Files.exists(avatarPath)) {
            return Optional.empty();
        }

        try {
            Resource resource = new UrlResource(avatarPath.toUri());
            MediaType mediaType = detectMediaType(avatarPath);
            return Optional.of(new AvatarResource(resource, mediaType));
        } catch (MalformedURLException e) {
            return Optional.empty();
        }
    }

    private String nullToEmpty(String text) {
        return text == null ? "" : text;
    }

    private String firstNonBlank(String preferred, String fallback) {
        String normalizedPreferred = preferred == null ? "" : preferred.trim();
        if (!normalizedPreferred.isBlank()) {
            return normalizedPreferred;
        }
        return nullToEmpty(fallback);
    }

    private String mergeText(String incoming, String current) {
        if (incoming == null) {
            return current;
        }
        return incoming.trim();
    }

    private Integer normalizeGender(Integer gender) {
        if (gender == null) {
            return 0;
        }
        return (gender >= 0 && gender <= 2) ? gender : 0;
    }

    private Integer mergeGender(Integer incoming, Integer current) {
        if (incoming == null) {
            return current;
        }
        if (incoming < 0 || incoming > 2) {
            return current;
        }
        return incoming;
    }

    private Path resolveUserAvatarDir(Long userId) {
        return Path.of(appProperties.getFile().getUploadDir(), "avatar", String.valueOf(userId)).toAbsolutePath().normalize();
    }

    private MediaType detectMediaType(Path path) {
        try {
            String type = Files.probeContentType(path);
            if (type == null || type.isBlank()) {
                return MediaType.APPLICATION_OCTET_STREAM;
            }
            return MediaType.parseMediaType(type);
        } catch (IOException ignored) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "avatar";
        }
        String cleaned = filename.replace("\\", "/");
        int slash = cleaned.lastIndexOf('/');
        String name = slash >= 0 ? cleaned.substring(slash + 1) : cleaned;
        String safe = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        return safe.isBlank() ? "avatar" : safe;
    }

    public record ProfileRequest(
            String realName,
            String bio,
            String institution,
            String department,
            String researchDirection,
            String degreeLevel,
            Integer gender,
            String avatarUrl
    ) {
    }

    public record AvatarResource(Resource resource, MediaType contentType) {
    }
}
