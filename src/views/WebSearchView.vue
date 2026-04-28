<template>
  <div class="page-grid">
    <SectionCard eyebrow="Source Search" title="基于抖音 / 小红书 / 网页搜索" subtitle="按参考日历页的整齐卡片分区与浅色边框节奏组织搜索流。">
      <template #actions>
        <div class="web-search__actions">
          <el-select v-model="selectedSource" class="app-input web-search__source" placeholder="选择来源">
            <el-option label="网页" value="网页" />
            <el-option label="抖音" value="抖音" />
            <el-option label="小红书" value="小红书" />
          </el-select>
          <el-input v-model="query" class="app-input web-search__input" placeholder="输入搜索主题" @keyup.enter="runSearch" />
          <GlassButton variant="primary" @click="runSearch">
            <template #icon>
              <Search style="width: 16px; height: 16px" />
            </template>
            搜索
          </GlassButton>
        </div>
      </template>

      <div class="mono-block">{{ store.webMarkdown }}</div>
      <div class="web-search__save">
        <GlassButton variant="ghost" @click="saveCurrent">
          <template #icon>
            <FolderAdd style="width: 16px; height: 16px" />
          </template>
          保存
        </GlassButton>
      </div>
    </SectionCard>

    <SectionCard eyebrow="History Search" title="历史搜索" subtitle="保留最近搜索来源、摘要片段和是否已保存。">
      <div class="history-list">
        <article v-for="item in store.webHistory" :key="item.id" class="history-list__item">
          <div>
            <div class="history-list__top">
              <strong>{{ item.query }}</strong>
              <span class="glass-chip">{{ item.source }}</span>
            </div>
            <p>{{ item.excerpt }}</p>
          </div>
          <GlassButton :variant="item.saved ? 'ghost' : 'default'" @click="store.saveWebHistoryItem(item.id)">
            <template #icon>
              <FolderAdd style="width: 16px; height: 16px" />
            </template>
            {{ item.saved ? '已保存' : '保存' }}
          </GlassButton>
        </article>
      </div>
    </SectionCard>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { FolderAdd, Search } from '@element-plus/icons-vue';

import GlassButton from '@/components/ui/GlassButton.vue';
import SectionCard from '@/components/ui/SectionCard.vue';
import { type SearchSource, useWorkspaceStore } from '@/stores/workspace';

const store = useWorkspaceStore();
const query = ref(store.webQuery);
const selectedSource = ref<SearchSource>(store.selectedSource);

const runSearch = () => {
  store.runWebSearch(query.value, selectedSource.value);
};

const saveCurrent = () => {
  const latest = store.webHistory[0];
  if (latest) {
    store.saveWebHistoryItem(latest.id);
  }
};
</script>

<style scoped>
.web-search__actions {
  display: flex;
  gap: 12px;
  min-width: 680px;
}

.web-search__source {
  width: 140px;
}

.web-search__input {
  width: 360px;
}

.web-search__save {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.history-list {
  display: grid;
  gap: 14px;
}

.history-list__top {
  display: flex;
  align-items: center;
  gap: 12px;
}

.history-list__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 18px;
  border-radius: 24px;
  border: 1px solid rgba(203, 212, 228, 0.72);
  background: rgba(248, 250, 255, 0.84);
}

.history-list__item p {
  margin: 10px 0 0;
  color: var(--text-soft);
  line-height: 1.7;
}

@media (max-width: 900px) {
  .web-search__actions,
  .history-list__item {
    flex-direction: column;
    align-items: stretch;
    min-width: 0;
  }

  .web-search__source,
  .web-search__input {
    width: 100%;
  }
}
</style>
