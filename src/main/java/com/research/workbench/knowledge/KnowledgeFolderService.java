package com.research.workbench.knowledge;

import com.research.workbench.auth.CurrentUserService;
import com.research.workbench.domain.KnowledgeBase;
import com.research.workbench.domain.KnowledgeFolder;
import com.research.workbench.repository.KnowledgeBaseRepository;
import com.research.workbench.repository.KnowledgeFolderRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class KnowledgeFolderService {

    private final KnowledgeFolderRepository knowledgeFolderRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final CurrentUserService currentUserService;

    public KnowledgeFolderService(
            KnowledgeFolderRepository knowledgeFolderRepository,
            KnowledgeBaseRepository knowledgeBaseRepository,
            CurrentUserService currentUserService
    ) {
        this.knowledgeFolderRepository = knowledgeFolderRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(Long kbId) {
        requireOwnedBase(kbId);
        return knowledgeFolderRepository.findByKbIdOrderBySortNoAscUpdatedAtDesc(kbId)
                .stream()
                .map(this::toFolderDto)
                .toList();
    }

    public Map<String, Object> create(Long kbId, String name, Long parentId) {
        requireOwnedBase(kbId);
        KnowledgeFolder folder = new KnowledgeFolder();
        folder.setKbId(kbId);
        folder.setParentId(parentId);
        folder.setName(name);
        folder = knowledgeFolderRepository.save(folder);
        return toFolderDto(folder);
    }

    public Map<String, Object> rename(Long id, String name) {
        KnowledgeFolder folder = knowledgeFolderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文件夹不存在: " + id));
        requireOwnedBase(folder.getKbId());
        folder.setName(name);
        folder = knowledgeFolderRepository.save(folder);
        return toFolderDto(folder);
    }

    public void delete(Long id) {
        KnowledgeFolder folder = knowledgeFolderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文件夹不存在: " + id));
        requireOwnedBase(folder.getKbId());
        knowledgeFolderRepository.delete(folder);
    }

    private void requireOwnedBase(Long kbId) {
        KnowledgeBase base = knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> new IllegalArgumentException("知识库不存在: " + kbId));
        if (!currentUserService.requireCurrentUserId().equals(base.getOwnerUserId())) {
            throw new IllegalArgumentException("无权访问该知识库");
        }
    }

    private Map<String, Object> toFolderDto(KnowledgeFolder folder) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", folder.getId());
        data.put("kbId", folder.getKbId());
        data.put("parentId", folder.getParentId());
        data.put("name", folder.getName());
        data.put("sortNo", folder.getSortNo());
        return data;
    }
}
