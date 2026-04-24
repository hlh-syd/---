package com.research.workbench.bootstrap;

import com.research.workbench.domain.AiChatMessage;
import com.research.workbench.domain.AiChatSession;
import com.research.workbench.domain.ResearchProject;
import com.research.workbench.domain.StudyTask;
import com.research.workbench.domain.SysUser;
import com.research.workbench.domain.UserProfile;
import com.research.workbench.domain.Workspace;
import com.research.workbench.repository.AiChatMessageRepository;
import com.research.workbench.repository.AiChatSessionRepository;
import com.research.workbench.repository.ResearchProjectRepository;
import com.research.workbench.repository.StudyTaskRepository;
import com.research.workbench.repository.UserProfileRepository;
import com.research.workbench.repository.WorkspaceRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserInitializationService {

    private final UserProfileRepository userProfileRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ResearchProjectRepository researchProjectRepository;
    private final AiChatSessionRepository aiChatSessionRepository;
    private final AiChatMessageRepository aiChatMessageRepository;
    private final StudyTaskRepository studyTaskRepository;

    public UserInitializationService(
            UserProfileRepository userProfileRepository,
            WorkspaceRepository workspaceRepository,
            ResearchProjectRepository researchProjectRepository,
            AiChatSessionRepository aiChatSessionRepository,
            AiChatMessageRepository aiChatMessageRepository,
            StudyTaskRepository studyTaskRepository
    ) {
        this.userProfileRepository = userProfileRepository;
        this.workspaceRepository = workspaceRepository;
        this.researchProjectRepository = researchProjectRepository;
        this.aiChatSessionRepository = aiChatSessionRepository;
        this.aiChatMessageRepository = aiChatMessageRepository;
        this.studyTaskRepository = studyTaskRepository;
    }

    public void ensureInitialized(SysUser user) {
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(user.getId());
            profile.setRealName(user.getNickname());
            profile.setBio("围绕研究问题沉淀资料、生成结论并持续推进执行。");
            profile.setInstitution("Your Lab");
            profile.setDepartment("Research Workspace");
            profile.setResearchDirection("Literature Review / Knowledge Base / AI Workflow");
            profile.setDegreeLevel("Researcher");
            profile.setInterests("[\"Research\",\"AI\",\"Workflow\"]");
            profile.setTags("[\"Research\",\"Workspace\",\"AI\"]");
            userProfileRepository.save(profile);
        }

        Workspace workspace = workspaceRepository.findByOwnerUserIdOrderByUpdatedAtDesc(user.getId()).stream().findFirst().orElse(null);
        if (workspace == null) {
            workspace = new Workspace();
            workspace.setOwnerUserId(user.getId());
            workspace.setName(user.getNickname() + " 的研究工作台");
            workspace.setDescription("把搜索、知识沉淀、AI 产出和计划推进拉到同一条工作主链。");
            workspace.setVisibility("PRIVATE");
            workspace.setSystemPrompt("优先围绕研究问题组织资料、结论和下一步行动。");
            workspace = workspaceRepository.save(workspace);
        }

        ResearchProject project = researchProjectRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspace.getId()).stream().findFirst().orElse(null);
        if (project == null) {
            project = new ResearchProject();
            project.setWorkspaceId(workspace.getId());
            project.setOwnerUserId(user.getId());
            project.setName("首次研究项目");
            project.setTopic("Workspace Onboarding");
            project.setSummary("这里用于沉淀你的第一批搜索结果、知识库文档和 AI 结论。");
            project.setColorToken("aurora");
            project.setStageCode("SEARCH");
            project = researchProjectRepository.save(project);
        }

        if (aiChatSessionRepository.findTopByUserIdOrderByUpdatedAtDesc(user.getId()).isEmpty()) {
            AiChatSession session = new AiChatSession();
            session.setUserId(user.getId());
            session.setWorkspaceId(workspace.getId());
            session.setProjectId(project.getId());
            session.setTitle("欢迎来到研究工作台");
            session.setAssistantType("GENERAL");
            session.setSourceDocIds("[]");
            session = aiChatSessionRepository.save(session);

            AiChatMessage message = new AiChatMessage();
            message.setSessionId(session.getId());
            message.setRoleCode("ASSISTANT");
            message.setModelName("system");
            message.setContentText("你的空间已经准备完成。接下来可以先发起一次研究搜索，或者创建第一个知识库。");
            aiChatMessageRepository.save(message);
        }

        if (studyTaskRepository.findTop20ByUserIdOrderByDueTimeAsc(user.getId()).isEmpty()) {
            StudyTask task = new StudyTask();
            task.setUserId(user.getId());
            task.setWorkspaceId(workspace.getId());
            task.setProjectId(project.getId());
            task.setTitle("完成首次登录后的工作台检查");
            task.setDescription("检查个人资料、发起一次搜索，并保存一条资料到知识库。");
            task.setPriorityLevel(2);
            task.setTaskStatus("TODO");
            task.setDueTime(LocalDateTime.now().plusDays(1));
            studyTaskRepository.save(task);
        }
    }
}
