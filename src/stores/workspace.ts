import { computed, ref } from 'vue';
import { defineStore } from 'pinia';

export type SearchSource = '网页' | '抖音' | '小红书';
export type ExternalLinkName = '微信' | '飞书';

export interface PaperResult {
  id: string;
  title: string;
  authors: string;
  year: string;
  journal: string;
  summary: string;
  tags: string[];
  citedBy: number;
  inKnowledgeBase: boolean;
}

export interface SearchHistoryItem {
  id: string;
  keyword: string;
  time: string;
  hits: number;
}

export interface MindMapNode {
  title: string;
  detail: string;
  accent?: boolean;
}

export interface KnowledgeFile {
  id: string;
  name: string;
  size: string;
  updatedAt: string;
  summary: string;
  selected: boolean;
}

export interface KnowledgeFolder {
  id: string;
  name: string;
  files: KnowledgeFile[];
}

export interface WebHistoryItem {
  id: string;
  source: SearchSource;
  query: string;
  excerpt: string;
  saved: boolean;
}

export interface AiTool {
  id: string;
  name: string;
  description: string;
  url: string;
  tag: string;
}

export interface PlanTask {
  id: string;
  title: string;
  owner: string;
  due: string;
  lane: string;
  progress: number;
}

export interface CalendarEvent {
  day: number;
  title: string;
  tag: string;
}

export interface PunchDay {
  day: string;
  done: boolean;
}

export interface ProfileDraft {
  name: string;
  title: string;
  institution: string;
  bio: string;
  avatar: string;
  links: Array<{
    name: ExternalLinkName;
    linked: boolean;
    account: string;
  }>;
}

const nextId = () => `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;

const defaultMarkdownBySource = (query: string, source: SearchSource) => {
  if (source === '抖音') {
    return `# ${query}\n\n- 热门切入点：3 秒内给出研究结论或实验现象。\n- 内容结构：问题抛出 -> 数据证据 -> 方法解释 -> 结论。\n- 适合转存到知识库的内容：实验步骤、耗材选择、失败案例。`;
  }

  if (source === '小红书') {
    return `# ${query}\n\n## 观察结论\n- 用户更关注“是否能直接复现”与“材料怎么买”。\n- 高互动内容偏向图文清单与对比表。\n\n## 可执行建议\n1. 输出实验配置清单。\n2. 增加避坑说明。\n3. 将关键词拆成品牌词与场景词。`;
  }

  return `# ${query}\n\n## 检索摘要\n- 网页结果显示该主题正在向“多模态综述 + 知识库沉淀”演进。\n- 高质量内容集中在综述、机构博客与标准文档。\n\n## 建议动作\n1. 抽取高频概念词。\n2. 标记可追溯来源。\n3. 将结构化摘要加入知识库。`;
};

export const useWorkspaceStore = defineStore('workspace', () => {
  const academicQuery = ref('生成式 AI 辅助生物医学综述写作');
  const paperResults = ref<PaperResult[]>([
    {
      id: 'paper-1',
      title: 'Retrieval-Augmented Literature Agents for Biomedical Review Writing',
      authors: 'L. Chen, M. Torres, Y. Huang',
      year: '2026',
      journal: 'Nature Machine Intelligence',
      summary: '将检索增强代理用于综述写作，强调来源追踪、引文对齐与结构化摘要融合。',
      tags: ['RAG', 'Biomedical', 'Survey Agent'],
      citedBy: 84,
      inKnowledgeBase: true,
    },
    {
      id: 'paper-2',
      title: 'Mind-Mapped Evidence Synthesis from Multi-PDF Scientific Corpora',
      authors: 'E. Rossi, J. Kim',
      year: '2025',
      journal: 'ACL Findings',
      summary: '利用层级图谱组织论文证据链，将主题、实验和结论映射到思维导图节点。',
      tags: ['Evidence Graph', 'Mind Map'],
      citedBy: 46,
      inKnowledgeBase: false,
    },
    {
      id: 'paper-3',
      title: 'Fast Abstractive Summaries for Review Planning with Citation Guarantees',
      authors: 'K. Singh, A. Doyle',
      year: '2025',
      journal: 'EMNLP',
      summary: '聚焦带引文约束的快速总结，适用于系统综述与文献调研首页摘要。',
      tags: ['Summarization', 'Citation'],
      citedBy: 67,
      inKnowledgeBase: false,
    },
  ]);

  const academicHistory = ref<SearchHistoryItem[]>([
    { id: 'history-1', keyword: '单细胞转录组知识库工作流', time: '09:30', hits: 32 },
    { id: 'history-2', keyword: '癌症免疫综述自动化总结', time: '昨天', hits: 19 },
    { id: 'history-3', keyword: '文献搜索到思维导图', time: '周六', hits: 11 },
  ]);

  const mindMapNodes = ref<MindMapNode[]>([
    { title: '论文搜索', detail: '先看综述、再抓方法、最后回查数据集。', accent: true },
    { title: '证据聚类', detail: '按主题、实验对象与评价指标拆分。 ' },
    { title: '快速总结', detail: '保留结论、边界条件与可复现实验要点。' },
    { title: '入库策略', detail: '结果页一键加入指定知识库文件夹。' },
  ]);

  const knowledgeFolders = ref<KnowledgeFolder[]>([
    {
      id: 'folder-1',
      name: '综述草稿',
      files: [
        {
          id: 'file-1',
          name: 'RAG-biomed-review.pdf',
          size: '2.4 MB',
          updatedAt: '今天 08:42',
          summary: '检索增强 + 证据对齐',
          selected: false,
        },
        {
          id: 'file-2',
          name: 'multi-agent-outline.md',
          size: '128 KB',
          updatedAt: '昨天 21:18',
          summary: '章节结构与写作提示',
          selected: false,
        },
      ],
    },
    {
      id: 'folder-2',
      name: '实验笔记',
      files: [
        {
          id: 'file-3',
          name: 'single-cell-notes.docx',
          size: '684 KB',
          updatedAt: '04-25',
          summary: '实验流程与参数记录',
          selected: false,
        },
      ],
    },
    {
      id: 'folder-3',
      name: '待清洗资料',
      files: [
        {
          id: 'file-4',
          name: 'web-clips-batch.zip',
          size: '8.7 MB',
          updatedAt: '04-23',
          summary: '待提炼来源与标签',
          selected: false,
        },
      ],
    },
  ]);

  const knowledgeQuestion = ref('根据“综述草稿”中的 PDF，总结 RAG 代理如何保证引文可信。');
  const knowledgeSearchKeyword = ref('citation');
  const knowledgeSummary = ref(
    '当前知识库更适合做“按主题聚合 + 按证据回查”的综述型工作流，推荐将方法、实验和限制条件拆成独立摘要块。',
  );
  const activeFolderId = ref('folder-1');

  const webQuery = ref('生物医学综述写作 AI 工具');
  const selectedSource = ref<SearchSource>('网页');
  const webMarkdown = ref(defaultMarkdownBySource(webQuery.value, selectedSource.value));
  const webHistory = ref<WebHistoryItem[]>([
    {
      id: 'web-1',
      source: '网页',
      query: 'Mermaid 流程图 文献总结',
      excerpt: '站点内容偏技术教程，适合作为流程图语法参考。',
      saved: true,
    },
    {
      id: 'web-2',
      source: '小红书',
      query: '文献调研工作流 模板',
      excerpt: '高互动内容强调模板化输出与分享复用。',
      saved: false,
    },
    {
      id: 'web-3',
      source: '抖音',
      query: '论文搜索 快速总结',
      excerpt: '短视频内容适合抓选题热点和工具对比。',
      saved: false,
    },
  ]);

  const aiTools = ref<AiTool[]>([
    {
      id: 'tool-1',
      name: 'OpenAlex Explorer',
      description: '适合文献元数据、作者图谱与引文网络初筛。',
      url: 'https://openalex.org',
      tag: '外部网页',
    },
    {
      id: 'tool-2',
      name: 'Semantic Scholar',
      description: '适合高质量论文相关推荐与引用脉络追踪。',
      url: 'https://www.semanticscholar.org',
      tag: '外部网页',
    },
    {
      id: 'tool-3',
      name: 'PubMed',
      description: 'Bio 生物医学方向核心入口，适合规范检索。',
      url: 'https://pubmed.ncbi.nlm.nih.gov',
      tag: '外部网页',
    },
  ]);

  const promptDraft = ref(
    '帮我总结上传的生物医学论文，并输出可以直接用于综述写作的结构化结论。',
  );

  const bioAssistantAnswer = ref(
    '建议使用疾病、实验对象、检测手段、关键终点四层框架组织生物医学问答，并始终附带来源段落。',
  );

  const planTasks = ref<PlanTask[]>([
    { id: 'plan-1', title: '完成综述引言段落', owner: '本人', due: '04-28', lane: '写作', progress: 72 },
    { id: 'plan-2', title: '补充 12 篇对比论文', owner: '本人', due: '04-29', lane: '检索', progress: 48 },
    { id: 'plan-3', title: '整理知识库标签结构', owner: '协作者', due: '04-30', lane: '整理', progress: 36 },
  ]);

  const calendarEvents = ref<CalendarEvent[]>([
    { day: 3, title: '论文搜索', tag: '调研' },
    { day: 8, title: '知识库总结', tag: '整理' },
    { day: 14, title: '番茄钟冲刺', tag: '专注' },
    { day: 18, title: '文献流程图输出', tag: '可视化' },
    { day: 26, title: '周回顾', tag: '复盘' },
  ]);

  const punchDays = ref<PunchDay[]>([
    { day: 'Mon', done: true },
    { day: 'Tue', done: true },
    { day: 'Wed', done: true },
    { day: 'Thu', done: false },
    { day: 'Fri', done: true },
    { day: 'Sat', done: false },
    { day: 'Sun', done: false },
  ]);

  const relaxFortune = ref(
    '今日宜做“检索归纳”，不宜一次性堆太多来源；先做结构，后补细节，效率更稳。',
  );
  const foodSuggestion = ref('番茄牛腩饭 + 无糖气泡水，适合长时间专注后补充能量。');

  const profile = ref<ProfileDraft>({
    name: '林知遥',
    title: '医学信息学研究者',
    institution: '临床数据与智能研究中心',
    bio: '关注生物医学知识库、综述自动化与研究工作流设计。',
    avatar:
      'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=320&q=80',
    links: [
      { name: '微信', linked: true, account: 'lin-research' },
      { name: '飞书', linked: false, account: '未关联' },
    ],
  });

  const navItems = [
    { name: 'research', label: '学术调研', accent: 'dashboard' },
    { name: 'knowledge', label: '知识库', accent: 'system' },
    { name: 'web-search', label: '网页搜索', accent: 'calendar' },
    { name: 'ai-services', label: 'AI服务', accent: 'chat' },
    { name: 'research-plan', label: '研究计划', accent: 'task' },
    { name: 'relax', label: '放松一下', accent: 'button' },
    { name: 'profile', label: '个人主页', accent: 'login' },
  ] as const;

  const activeFolder = computed(
    () => knowledgeFolders.value.find((folder) => folder.id === activeFolderId.value) ?? knowledgeFolders.value[0],
  );

  const promptOptimized = computed(() => {
    return [
      '你是一名严谨的生物医学综述助手。',
      '任务：从上传文献中提取研究问题、方法、结果、限制和可引用结论。',
      '输出要求：使用分级标题、要点列表与“证据来源”字段。',
      '限制：禁止编造数据；每段结论后附来源标识；如果证据不足要明确说明。',
    ].join('\n');
  });

  const knowledgeStats = computed(() => {
    const fileCount = knowledgeFolders.value.reduce((count, folder) => count + folder.files.length, 0);
    return {
      folderCount: knowledgeFolders.value.length,
      fileCount,
      summaryCount: paperResults.value.filter((paper) => paper.inKnowledgeBase).length,
    };
  });

  const togglePaperCollection = (paperId: string) => {
    const paper = paperResults.value.find((item) => item.id === paperId);
    if (!paper) {
      return;
    }

    paper.inKnowledgeBase = !paper.inKnowledgeBase;

    if (paper.inKnowledgeBase) {
      activeFolder.value.files.unshift({
        id: nextId(),
        name: `${paper.title.slice(0, 28)}.md`,
        size: '46 KB',
        updatedAt: '刚刚',
        summary: '由论文搜索结果一键加入',
        selected: false,
      });
    }
  };

  const runAcademicSearch = (keyword: string) => {
    academicQuery.value = keyword.trim() || academicQuery.value;
    academicHistory.value.unshift({
      id: nextId(),
      keyword: academicQuery.value,
      time: '刚刚',
      hits: paperResults.value.length,
    });
  };

  const addFolder = (name: string) => {
    const cleanName = name.trim();
    if (!cleanName) {
      return;
    }

    knowledgeFolders.value.unshift({
      id: nextId(),
      name: cleanName,
      files: [],
    });
    activeFolderId.value = knowledgeFolders.value[0].id;
  };

  const renameFolder = (folderId: string, name: string) => {
    const folder = knowledgeFolders.value.find((item) => item.id === folderId);
    if (!folder || !name.trim()) {
      return;
    }

    folder.name = name.trim();
  };

  const removeFolder = (folderId: string) => {
    if (knowledgeFolders.value.length === 1) {
      return;
    }

    knowledgeFolders.value = knowledgeFolders.value.filter((folder) => folder.id !== folderId);
    activeFolderId.value = knowledgeFolders.value[0].id;
  };

  const addFilesToActiveFolder = (fileNames: string[]) => {
    const files = fileNames
      .filter(Boolean)
      .map((name) => ({
        id: nextId(),
        name,
        size: 'New',
        updatedAt: '刚刚',
        summary: '新上传文件，待总结',
        selected: false,
      }));
    activeFolder.value.files.unshift(...files);
  };

  const toggleFileSelection = (fileId: string) => {
    const target = activeFolder.value.files.find((file) => file.id === fileId);
    if (!target) {
      return;
    }

    target.selected = !target.selected;
  };

  const deleteFile = (fileId: string) => {
    activeFolder.value.files = activeFolder.value.files.filter((file) => file.id !== fileId);
  };

  const deleteSelectedFiles = () => {
    activeFolder.value.files = activeFolder.value.files.filter((file) => !file.selected);
  };

  const runWebSearch = (query: string, source: SearchSource) => {
    const cleanQuery = query.trim() || webQuery.value;
    webQuery.value = cleanQuery;
    selectedSource.value = source;
    webMarkdown.value = defaultMarkdownBySource(cleanQuery, source);
    webHistory.value.unshift({
      id: nextId(),
      source,
      query: cleanQuery,
      excerpt: `已根据 ${source} 结果输出 Markdown 摘要。`,
      saved: false,
    });
  };

  const saveWebHistoryItem = (historyId: string) => {
    const item = webHistory.value.find((history) => history.id === historyId);
    if (!item) {
      return;
    }
    item.saved = true;
  };

  const toggleExternalLink = (name: ExternalLinkName) => {
    const link = profile.value.links.find((item) => item.name === name);
    if (!link) {
      return;
    }

    link.linked = !link.linked;
    link.account = link.linked ? `${name.toLowerCase()}-research` : '未关联';
  };

  const updateAvatar = (avatar: string) => {
    profile.value.avatar = avatar;
  };

  return {
    academicHistory,
    academicQuery,
    activeFolder,
    activeFolderId,
    aiTools,
    bioAssistantAnswer,
    calendarEvents,
    foodSuggestion,
    knowledgeFolders,
    knowledgeQuestion,
    knowledgeSearchKeyword,
    knowledgeStats,
    knowledgeSummary,
    mindMapNodes,
    navItems,
    paperResults,
    planTasks,
    profile,
    promptDraft,
    promptOptimized,
    punchDays,
    relaxFortune,
    selectedSource,
    webHistory,
    webMarkdown,
    webQuery,
    addFilesToActiveFolder,
    addFolder,
    deleteFile,
    deleteSelectedFiles,
    removeFolder,
    renameFolder,
    runAcademicSearch,
    runWebSearch,
    saveWebHistoryItem,
    toggleExternalLink,
    toggleFileSelection,
    togglePaperCollection,
    updateAvatar,
  };
});
