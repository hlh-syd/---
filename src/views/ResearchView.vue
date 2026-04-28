<template>
  <div class="page-grid">
    <SectionCard
      eyebrow="Dashboard"
      title="学术调研总览"
      subtitle="用仪表盘参考稿的统计卡、交叉筛选和干净图表节奏承接论文搜索首页。"
    >
      <div class="page-grid grid-4">
        <article class="metric-card">
          <span class="metric-card__label">论文搜索</span>
          <div class="metric-card__value">{{ store.paperResults.length }}</div>
          <div class="metric-card__meta">当前检索结果</div>
        </article>
        <article class="metric-card">
          <span class="metric-card__label">思维导图</span>
          <div class="metric-card__value">{{ store.mindMapNodes.length }}</div>
          <div class="metric-card__meta">已生成节点</div>
        </article>
        <article class="metric-card">
          <span class="metric-card__label">快速总结</span>
          <div class="metric-card__value">03</div>
          <div class="metric-card__meta">可直接转综述段落</div>
        </article>
        <article class="metric-card">
          <span class="metric-card__label">知识库沉淀</span>
          <div class="metric-card__value">{{ store.knowledgeStats.summaryCount }}</div>
          <div class="metric-card__meta">已一键加入</div>
        </article>
      </div>

      <div class="soft-chart research-chart">
        <svg viewBox="0 0 640 220" fill="none">
          <path d="M40 160 C90 150 110 72 160 80 C220 90 220 172 280 168 C340 164 368 52 430 62 C500 74 530 126 600 110" stroke="#316DFF" stroke-linecap="round" stroke-width="5" />
          <path d="M40 182 C86 182 128 128 180 130 C236 132 244 192 298 192 C352 192 386 102 444 110 C500 118 544 150 600 146" stroke="#19B7A0" stroke-linecap="round" stroke-width="5" opacity="0.76" />
          <circle cx="430" cy="62" r="10" fill="#316DFF" />
          <circle cx="444" cy="110" r="10" fill="#19B7A0" />
        </svg>
        <div class="research-chart__legend">
          <span class="glass-chip"><i class="research-chart__dot research-chart__dot--blue"></i> 相关度</span>
          <span class="glass-chip"><i class="research-chart__dot research-chart__dot--teal"></i> 可入库性</span>
        </div>
      </div>
    </SectionCard>

    <div class="split-card">
      <SectionCard eyebrow="Paper Search" title="论文搜索" subtitle="保持白底高密度结果列表，用于快速判断是否需要入库。">
        <template #actions>
          <div class="research-search__tools">
            <el-input v-model="searchKeyword" class="app-input" placeholder="输入论文主题或问题" @keyup.enter="runSearch" />
            <GlassButton variant="primary" @click="runSearch">
              <template #icon>
                <Search style="width: 16px; height: 16px" />
              </template>
              搜索
            </GlassButton>
          </div>
        </template>

        <div class="research-search__list">
          <article v-for="paper in store.paperResults" :key="paper.id" class="research-search__item">
            <div class="research-search__item-head">
              <div>
                <h3>{{ paper.title }}</h3>
                <p>{{ paper.authors }} · {{ paper.journal }} · {{ paper.year }}</p>
              </div>
              <GlassButton :variant="paper.inKnowledgeBase ? 'ghost' : 'default'" @click="store.togglePaperCollection(paper.id)">
                <template #icon>
                  <CollectionTag style="width: 16px; height: 16px" />
                </template>
                {{ paper.inKnowledgeBase ? '已加入知识库' : '一键加入知识库' }}
              </GlassButton>
            </div>
            <p class="research-search__summary">{{ paper.summary }}</p>
            <div class="research-search__meta">
              <div class="research-search__tags">
                <span v-for="tag in paper.tags" :key="tag" class="glass-chip">{{ tag }}</span>
              </div>
              <span class="status-badge status-badge--amber">引用 {{ paper.citedBy }}</span>
            </div>
          </article>
        </div>
      </SectionCard>

      <div class="responsive-stack">
        <SectionCard eyebrow="Mind Map" title="思维导图" subtitle="采用流程图稿的区块关系，把检索结果压缩成层级节点。">
          <div class="mindmap">
            <div v-for="node in store.mindMapNodes" :key="node.title" class="mindmap__node" :class="{ 'mindmap__node--accent': node.accent }">
              <strong>{{ node.title }}</strong>
              <span>{{ node.detail }}</span>
            </div>
          </div>
        </SectionCard>

        <SectionCard eyebrow="Quick Summary" title="快速总结" subtitle="保留适合直接写进综述和组会的摘要句式。">
          <div class="research-summary">
            <article class="research-summary__block">
              <span class="status-badge">研究问题</span>
              <p>现有文献正从“单轮搜索”转向“检索、聚类、总结、入库”一体化研究台。</p>
            </article>
            <article class="research-summary__block">
              <span class="status-badge status-badge--amber">方法趋势</span>
              <p>高质量方案都会把思维导图和流程图作为中间层，用于组织证据与章节结构。</p>
            </article>
            <article class="research-summary__block">
              <span class="status-badge status-badge--rose">限制条件</span>
              <p>如果知识库中的来源没有被结构化，后续问答和总结的质量会明显下降。</p>
            </article>
          </div>
        </SectionCard>
      </div>
    </div>

    <div class="page-grid grid-2">
      <SectionCard eyebrow="Knowledge Shortcut" title="一键加入知识库" subtitle="将当前最相关论文直接落入指定文件夹，保留来源与摘要。">
        <div class="research-add">
          <div class="research-add__highlight">
            <h3>{{ store.paperResults[0].title }}</h3>
            <p>{{ store.paperResults[0].summary }}</p>
          </div>
          <div class="research-add__controls">
            <el-select v-model="store.activeFolderId" class="app-input" placeholder="选择知识库文件夹">
              <el-option v-for="folder in store.knowledgeFolders" :key="folder.id" :label="folder.name" :value="folder.id" />
            </el-select>
            <GlassButton variant="primary" @click="store.togglePaperCollection(store.paperResults[0].id)">
              <template #icon>
                <CollectionTag style="width: 16px; height: 16px" />
              </template>
              加入当前文件夹
            </GlassButton>
          </div>
        </div>
      </SectionCard>

      <SectionCard eyebrow="History" title="历史搜索" subtitle="以轻量记录卡保存最近的检索意图和结果数量。">
        <ul class="list-reset history-list">
          <li v-for="item in store.academicHistory" :key="item.id" class="history-list__item">
            <div>
              <strong>{{ item.keyword }}</strong>
              <p>{{ item.time }} · 命中 {{ item.hits }} 篇</p>
            </div>
            <GlassButton variant="ghost" @click="replayHistory(item.keyword)">
              <template #icon>
                <RefreshRight style="width: 16px; height: 16px" />
              </template>
              再次搜索
            </GlassButton>
          </li>
        </ul>
      </SectionCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { CollectionTag, RefreshRight, Search } from '@element-plus/icons-vue';

import GlassButton from '@/components/ui/GlassButton.vue';
import SectionCard from '@/components/ui/SectionCard.vue';
import { useWorkspaceStore } from '@/stores/workspace';

const store = useWorkspaceStore();
const searchKeyword = ref(store.academicQuery);

const runSearch = () => {
  store.runAcademicSearch(searchKeyword.value);
};

const replayHistory = (keyword: string) => {
  searchKeyword.value = keyword;
  store.runAcademicSearch(keyword);
};
</script>

<style scoped>
.research-chart {
  margin-top: 24px;
}

.research-chart svg {
  width: 100%;
  height: 220px;
}

.research-chart__legend {
  display: flex;
  gap: 10px;
}

.research-chart__dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
}

.research-chart__dot--blue {
  background: var(--brand);
}

.research-chart__dot--teal {
  background: var(--teal);
}

.research-search__tools {
  display: flex;
  gap: 12px;
}

.research-search__list {
  display: grid;
  gap: 16px;
}

.research-search__item {
  padding: 20px;
  border-radius: 28px;
  border: 1px solid rgba(203, 212, 228, 0.72);
  background: rgba(248, 250, 255, 0.8);
}

.research-search__item-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.research-search__item h3 {
  margin: 0;
  font-size: 22px;
  line-height: 1.35;
}

.research-search__item p {
  margin: 8px 0 0;
  color: var(--text-soft);
}

.research-search__summary {
  margin-top: 18px;
  line-height: 1.75;
}

.research-search__meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-top: 18px;
}

.research-search__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.mindmap {
  display: grid;
  gap: 14px;
}

.mindmap__node {
  position: relative;
  padding: 18px 18px 18px 24px;
  border-radius: 24px;
  border: 1px solid rgba(204, 213, 232, 0.76);
  background: rgba(250, 252, 255, 0.92);
}

.mindmap__node::before {
  content: '';
  position: absolute;
  left: 12px;
  top: 20px;
  width: 4px;
  height: calc(100% - 40px);
  border-radius: 999px;
  background: rgba(25, 183, 160, 0.18);
}

.mindmap__node--accent::before {
  background: rgba(49, 109, 255, 0.5);
}

.mindmap__node strong {
  display: block;
  font-size: 18px;
  font-family: 'Manrope', 'Inter', sans-serif;
}

.mindmap__node span {
  display: block;
  margin-top: 8px;
  color: var(--text-soft);
  line-height: 1.7;
}

.research-summary {
  display: grid;
  gap: 14px;
}

.research-summary__block {
  padding: 18px;
  border-radius: 24px;
  background: rgba(248, 250, 255, 0.84);
  border: 1px solid rgba(205, 213, 228, 0.72);
}

.research-summary__block p {
  margin: 14px 0 0;
  color: var(--text-soft);
  line-height: 1.75;
}

.research-add {
  display: grid;
  gap: 16px;
}

.research-add__highlight {
  padding: 20px;
  border-radius: 28px;
  background: linear-gradient(180deg, rgba(68, 111, 255, 0.1), rgba(25, 183, 160, 0.08));
  border: 1px solid rgba(168, 190, 255, 0.44);
}

.research-add__highlight h3 {
  margin: 0;
  font-size: 24px;
}

.research-add__highlight p {
  margin: 12px 0 0;
  color: var(--text-soft);
  line-height: 1.75;
}

.research-add__controls {
  display: grid;
  gap: 12px;
}

.history-list {
  display: grid;
  gap: 14px;
}

.history-list__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  border-radius: 24px;
  background: rgba(248, 250, 255, 0.84);
  border: 1px solid rgba(205, 213, 228, 0.72);
}

.history-list__item p {
  margin: 8px 0 0;
  color: var(--text-faint);
}

@media (max-width: 900px) {
  .research-search__tools,
  .research-search__item-head,
  .research-search__meta,
  .history-list__item {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
