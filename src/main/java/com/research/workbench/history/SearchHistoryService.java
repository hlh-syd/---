package com.research.workbench.history;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.research.workbench.domain.SearchHistoryDetail;
import com.research.workbench.domain.SearchHistorySession;
import com.research.workbench.repository.SearchHistoryDetailRepository;
import com.research.workbench.repository.SearchHistorySessionRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class SearchHistoryService {

    private final SearchHistorySessionRepository searchHistorySessionRepository;
    private final SearchHistoryDetailRepository searchHistoryDetailRepository;
    private final ObjectMapper objectMapper;

    public SearchHistoryService(
            SearchHistorySessionRepository searchHistorySessionRepository,
            SearchHistoryDetailRepository searchHistoryDetailRepository,
            ObjectMapper objectMapper
    ) {
        this.searchHistorySessionRepository = searchHistorySessionRepository;
        this.searchHistoryDetailRepository = searchHistoryDetailRepository;
        this.objectMapper = objectMapper;
    }

    public void ensureSession(String sessionId, Long userId, String userName, String bizType, String title) {
        saveSession(sessionId, userId, userName, bizType, title);
    }

    public void appendDetail(
            String sessionId,
            Long userId,
            String userName,
            String bizType,
            String title,
            Object query,
            Object answer,
            Object extra
    ) {
        SearchHistorySession session = saveSession(sessionId, userId, userName, bizType, title);

        SearchHistoryDetail detail = new SearchHistoryDetail();
        detail.setSessionId(session.getSessionId());
        detail.setUserId(userId);
        detail.setUserName(normalizeUserName(userName));
        detail.setDetailNo((int) searchHistoryDetailRepository.countBySessionId(sessionId) + 1);
        detail.setQueryText(toText(query));
        detail.setAnswerText(toText(answer));
        detail.setExtraJson(toJson(extra));
        searchHistoryDetailRepository.save(detail);

        session.setItemCount(detail.getDetailNo());
        searchHistorySessionRepository.save(session);
    }

    private SearchHistorySession saveSession(String sessionId, Long userId, String userName, String bizType, String title) {
        SearchHistorySession session = searchHistorySessionRepository.findBySessionId(sessionId).orElse(null);
        if (session == null) {
            session = new SearchHistorySession();
            session.setSessionId(sessionId);
            session.setUserId(userId);
            session.setUserName(normalizeUserName(userName));
            session.setBizType(defaultBizType(bizType));
            session.setTitle(trimToLength(title, 255));
            return searchHistorySessionRepository.save(session);
        }

        boolean changed = false;
        String normalizedUserName = normalizeUserName(userName);
        String normalizedBizType = defaultBizType(bizType);
        String normalizedTitle = trimToLength(title, 255);
        if (!Objects.equals(session.getUserId(), userId)) {
            session.setUserId(userId);
            changed = true;
        }
        if (!Objects.equals(session.getUserName(), normalizedUserName)) {
            session.setUserName(normalizedUserName);
            changed = true;
        }
        if (!Objects.equals(session.getBizType(), normalizedBizType)) {
            session.setBizType(normalizedBizType);
            changed = true;
        }
        if (StringUtils.hasText(normalizedTitle) && !Objects.equals(session.getTitle(), normalizedTitle)) {
            session.setTitle(normalizedTitle);
            changed = true;
        }
        return changed ? searchHistorySessionRepository.save(session) : session;
    }

    private String normalizeUserName(String userName) {
        return StringUtils.hasText(userName) ? userName.trim() : "unknown";
    }

    private String defaultBizType(String bizType) {
        return StringUtils.hasText(bizType) ? bizType.trim() : "SEARCH";
    }

    private String trimToLength(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String normalized = text.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private String toText(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String text) {
            return text;
        }
        return toJson(value);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }
}
