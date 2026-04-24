package com.research.workbench.profile;

import com.research.workbench.domain.SysUser;
import com.research.workbench.domain.UserProfile;
import com.research.workbench.repository.SysUserRepository;
import com.research.workbench.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:profile-persistence;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class ProfilePersistenceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SysUserRepository sysUserRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private SysUser user;

    @BeforeEach
    void setUp() {
        user = sysUserRepository.findByUsernameIgnoreCase("alice").orElseGet(() -> {
            SysUser entity = new SysUser();
            entity.setUsername("alice");
            entity.setNickname("Old Nickname");
            entity.setEmail("alice@example.com");
            entity.setPasswordHash(passwordEncoder.encode("password123"));
            entity.setRoleCode("USER");
            entity.setStatus(1);
            return sysUserRepository.save(entity);
        });

        userProfileRepository.findByUserId(user.getId()).orElseGet(() -> {
            UserProfile profile = new UserProfile();
            profile.setUserId(user.getId());
            profile.setRealName("Old Name");
            profile.setInstitution("Old Institute");
            profile.setDepartment("Old Department");
            profile.setResearchDirection("Old Direction");
            profile.setDegreeLevel("Master");
            profile.setBio("Old Bio");
            return userProfileRepository.save(profile);
        });
    }

    @Test
    void profileUpdatePersistsAndBootstrapReturnsLatestValues() throws Exception {
        mockMvc.perform(put("/api/profile")
                        .with(user("alice").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "realName": "Alice Zhang",
                                  "institution": "New Institute",
                                  "department": "Precision Medicine",
                                  "degreeLevel": "PhD",
                                  "researchDirection": "Cancer Biomarkers",
                                  "bio": "Updated bio"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.realName").value("Alice Zhang"))
                .andExpect(jsonPath("$.data.displayName").value("Alice Zhang"))
                .andExpect(jsonPath("$.data.department").value("Precision Medicine"))
                .andExpect(jsonPath("$.data.degreeLevel").value("PhD"))
                .andExpect(jsonPath("$.data.researchDirection").value("Cancer Biomarkers"));

        UserProfile savedProfile = userProfileRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(savedProfile.getRealName()).isEqualTo("Alice Zhang");
        assertThat(savedProfile.getInstitution()).isEqualTo("New Institute");
        assertThat(savedProfile.getDepartment()).isEqualTo("Precision Medicine");
        assertThat(savedProfile.getDegreeLevel()).isEqualTo("PhD");
        assertThat(savedProfile.getResearchDirection()).isEqualTo("Cancer Biomarkers");
        assertThat(savedProfile.getBio()).isEqualTo("Updated bio");

        mockMvc.perform(get("/api/workbench/bootstrap")
                        .with(user("alice").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.user.name").value("Alice Zhang"))
                .andExpect(jsonPath("$.data.profile.user.institution").value("New Institute"))
                .andExpect(jsonPath("$.data.profile.detail.realName").value("Alice Zhang"))
                .andExpect(jsonPath("$.data.profile.detail.department").value("Precision Medicine"))
                .andExpect(jsonPath("$.data.profile.detail.degreeLevel").value("PhD"))
                .andExpect(jsonPath("$.data.profile.detail.researchDirection").value("Cancer Biomarkers"));
    }

    @Test
    void blankRealNameFallsBackToNicknameInDisplayName() throws Exception {
        mockMvc.perform(put("/api/profile")
                        .with(user("alice").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "realName": "",
                                  "institution": "New Institute",
                                  "department": "Precision Medicine",
                                  "degreeLevel": "PhD",
                                  "researchDirection": "Cancer Biomarkers",
                                  "bio": "Updated bio"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.realName").value(""))
                .andExpect(jsonPath("$.data.displayName").value("Old Nickname"));

        UserProfile savedProfile = userProfileRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(savedProfile.getRealName()).isEmpty();

        mockMvc.perform(get("/api/workbench/bootstrap")
                        .with(user("alice").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.user.name").value("Old Nickname"))
                .andExpect(jsonPath("$.data.profile.detail.realName").value(""));
    }
}
