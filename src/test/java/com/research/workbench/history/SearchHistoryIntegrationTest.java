package com.research.workbench.history;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.research.workbench.domain.SearchHistoryDetail;
import com.research.workbench.domain.SearchHistorySession;
import com.research.workbench.domain.SysUser;
import com.research.workbench.repository.SearchHistoryDetailRepository;
import com.research.workbench.repository.SearchHistorySessionRepository;
import com.research.workbench.repository.SysUserRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:search-history;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class SearchHistoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SysUserRepository sysUserRepository;

    @Autowired
    private SearchHistorySessionRepository searchHistorySessionRepository;

    @Autowired
    private SearchHistoryDetailRepository searchHistoryDetailRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private SysUser userEntity;

    @BeforeEach
    void setUp() {
        userEntity = sysUserRepository.findByUsernameIgnoreCase("alice").orElseGet(() -> {
            SysUser entity = new SysUser();
            entity.setUsername("alice");
            entity.setNickname("Alice");
            entity.setEmail("alice-search@example.com");
            entity.setPasswordHash(passwordEncoder.encode("password123"));
            entity.setRoleCode("USER");
            entity.setStatus(1);
            return sysUserRepository.save(entity);
        });
    }

    @Test
    void searchAndChatFlowsPersistUnifiedHistory() throws Exception {
        mockMvc.perform(get("/api/workbench/bootstrap")
                        .with(user("alice").roles("USER")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/workbench/research/query")
                        .with(user("alice").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "cancer biomarker",
                                  "platform": ""
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/workbench/web/query")
                        .with(user("alice").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "spatial transcriptomics workflow",
                                  "platform": "web"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/workbench/assistant/chat")
                        .with(user("alice").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prompt": "give me a next-step plan",
                                  "sources": ["paper-a.pdf", "notes.md"]
                                }
                                """))
                .andExpect(status().isOk());

        Long kbId = createKnowledgeBase();
        Long kbSessionId = createKnowledgeSession(kbId);

        mockMvc.perform(post("/api/knowledge/sessions/" + kbSessionId + "/messages")
                        .with(user("alice").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "what content is in this knowledge base?"
                                }
                                """))
                .andExpect(status().isOk());

        List<SearchHistorySession> sessions = searchHistorySessionRepository.findAll();
        List<SearchHistoryDetail> details = searchHistoryDetailRepository.findAll();
        Map<String, SearchHistorySession> sessionByType = sessions.stream()
                .collect(Collectors.toMap(SearchHistorySession::getBizType, Function.identity()));

        assertThat(sessionByType.keySet()).contains("RESEARCH", "WEB", "GENERAL_CHAT", "KB_CHAT");
        assertThat(sessionByType.get("RESEARCH").getUserId()).isEqualTo(userEntity.getId());
        assertThat(sessionByType.get("RESEARCH").getUserName()).isEqualTo("Alice");
        assertThat(sessionByType.get("RESEARCH").getItemCount()).isEqualTo(1);
        assertThat(sessionByType.get("WEB").getItemCount()).isEqualTo(1);
        assertThat(sessionByType.get("GENERAL_CHAT").getItemCount()).isEqualTo(1);
        assertThat(sessionByType.get("KB_CHAT").getItemCount()).isEqualTo(1);

        assertThat(details).hasSize(4);
        assertThat(details)
                .extracting(SearchHistoryDetail::getUserId)
                .containsOnly(userEntity.getId());
        assertThat(details)
                .extracting(SearchHistoryDetail::getUserName)
                .containsOnly("Alice");
        assertThat(details)
                .extracting(SearchHistoryDetail::getQueryText)
                .anyMatch(text -> text.contains("cancer biomarker"))
                .anyMatch(text -> text.contains("spatial transcriptomics workflow"))
                .anyMatch(text -> text.contains("give me a next-step plan"))
                .anyMatch(text -> text.contains("what content is in this knowledge base?"));
        assertThat(details)
                .extracting(SearchHistoryDetail::getSessionId)
                .allMatch(sessionId -> sessionId.startsWith("RESEARCH-")
                        || sessionId.startsWith("WEB-")
                        || sessionId.startsWith("GENERAL-")
                        || sessionId.startsWith("KB-"));
    }

    private Long createKnowledgeBase() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/knowledge/bases")
                        .with(user("alice").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test KB",
                                  "description": "history verification"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result).path("id").asLong();
    }

    private Long createKnowledgeSession(Long kbId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/knowledge/bases/" + kbId + "/sessions")
                        .with(user("alice").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "History Session"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result).path("id").asLong();
    }

    private JsonNode readData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }
}
