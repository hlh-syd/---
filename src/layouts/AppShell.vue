<template>
  <div class="app-shell">
    <aside class="shell-sidebar glass-panel sidebar">
      <div class="sidebar__header">
        <div class="sidebar__brand-mark"></div>
        <div>
          <div class="sidebar__brand-name">Research Canvas</div>
          <p class="sidebar__brand-copy">系统页采用企业控制台布局，按钮、聊天与流程图区域按参考稿映射。</p>
        </div>
      </div>

      <div class="sidebar__overview surface-card">
        <span class="eyebrow">Workspace</span>
        <div class="inline-kpi">
          <strong>{{ store.knowledgeStats.fileCount }}</strong>
          <span class="muted">份在库文档</span>
        </div>
        <div class="sidebar__overview-grid">
          <div>
            <div class="muted">文件夹</div>
            <strong>{{ store.knowledgeStats.folderCount }}</strong>
          </div>
          <div>
            <div class="muted">已沉淀总结</div>
            <strong>{{ store.knowledgeStats.summaryCount }}</strong>
          </div>
        </div>
      </div>

      <nav class="sidebar__nav">
        <RouterLink
          v-for="item in store.navItems"
          :key="item.name"
          class="sidebar__nav-item"
          :class="{ 'sidebar__nav-item--active': route.name === item.name }"
          :to="{ name: item.name }"
        >
          <div class="sidebar__nav-icon">
            <component :is="iconMap[item.name]" />
          </div>
          <div>
            <div class="sidebar__nav-label">{{ item.label }}</div>
            <div class="sidebar__nav-accent">{{ accentMap[item.accent] }}</div>
          </div>
        </RouterLink>
      </nav>

      <div class="sidebar__note surface-card">
        <div class="status-badge">Figma 映射完成</div>
        <p>
          登录页、按钮、系统壳、日历、任务、仪表盘、AI 对话与流程图页面已被映射到当前前端架构。
        </p>
      </div>
    </aside>

    <section class="shell-main">
      <header class="glass-panel shell-main__header">
        <div>
          <span class="eyebrow">System Page</span>
          <h1>{{ currentTitle }}</h1>
          <p>{{ currentDescription }}</p>
        </div>

        <div class="shell-main__actions">
          <el-input v-model="workspaceSearch" class="app-input shell-main__search" placeholder="搜索页面、功能或知识库内容" />
          <GlassButton variant="ghost">
            <template #icon>
              <Bell style="width: 16px; height: 16px" />
            </template>
            通知中心
          </GlassButton>
          <GlassButton variant="primary">
            <template #icon>
              <Plus style="width: 16px; height: 16px" />
            </template>
            新建工作流
          </GlassButton>
        </div>
      </header>

      <main class="shell-main__view">
        <RouterView />
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { RouterLink, RouterView, useRoute } from 'vue-router';
import {
  Bell,
  Collection,
  Compass,
  DataAnalysis,
  DocumentAdd,
  Food,
  Plus,
  User,
} from '@element-plus/icons-vue';

import GlassButton from '@/components/ui/GlassButton.vue';
import { useWorkspaceStore } from '@/stores/workspace';

const route = useRoute();
const store = useWorkspaceStore();
const workspaceSearch = ref('');

const currentTitle = computed(() => String(route.meta.label ?? '学术调研'));
const currentDescription = computed(() => String(route.meta.description ?? ''));

const iconMap = {
  research: DataAnalysis,
  knowledge: Collection,
  'web-search': Compass,
  'ai-services': DocumentAdd,
  'research-plan': Bell,
  relax: Food,
  profile: User,
} as const;

const accentMap = {
  dashboard: 'Dashboard Tone',
  system: 'Enterprise Shell',
  calendar: 'Calendar Grid',
  chat: 'Liquid Glass',
  task: 'Gantt Rhythm',
  button: 'Glass Button',
  login: 'Hero Login',
} as const;
</script>

<style scoped>
.sidebar {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 22px;
  border-radius: 34px;
}

.sidebar__header {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.sidebar__brand-mark {
  width: 56px;
  height: 56px;
  border-radius: 20px;
  background:
    radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.85), rgba(255, 255, 255, 0) 52%),
    linear-gradient(160deg, #427bff, #21c4a4);
  box-shadow:
    inset 0 1px 1px rgba(255, 255, 255, 0.8),
    0 18px 36px rgba(61, 102, 250, 0.24);
}

.sidebar__brand-name {
  font-family: 'Manrope', 'Inter', sans-serif;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.06em;
}

.sidebar__brand-copy {
  margin: 8px 0 0;
  color: var(--text-soft);
  line-height: 1.65;
}

.sidebar__overview {
  padding: 18px;
  border-radius: 28px;
}

.sidebar__overview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.sidebar__overview-grid strong {
  display: block;
  margin-top: 6px;
  font-size: 26px;
  font-family: 'Manrope', 'Inter', sans-serif;
}

.sidebar__nav {
  display: grid;
  gap: 12px;
}

.sidebar__nav-item {
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr);
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 24px;
  border: 1px solid transparent;
  background: rgba(255, 255, 255, 0.4);
  transition:
    transform 0.2s ease,
    background 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.sidebar__nav-item:hover,
.sidebar__nav-item--active {
  transform: translateX(2px);
  border-color: rgba(120, 144, 197, 0.25);
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 18px 28px rgba(15, 23, 42, 0.07);
}

.sidebar__nav-icon {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(247, 249, 255, 0.98), rgba(231, 237, 251, 0.78));
  color: var(--brand);
}

.sidebar__nav-icon :deep(svg) {
  width: 20px;
  height: 20px;
}

.sidebar__nav-label {
  font-weight: 700;
}

.sidebar__nav-accent {
  margin-top: 4px;
  color: var(--text-faint);
  font-size: 12px;
}

.sidebar__note {
  margin-top: auto;
  padding: 18px;
  border-radius: 28px;
}

.sidebar__note p {
  margin: 14px 0 0;
  color: var(--text-soft);
  line-height: 1.7;
}

.shell-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.shell-main__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
  padding: 24px 28px;
  border-radius: 34px;
}

.shell-main__header h1 {
  margin: 16px 0 0;
  font-family: 'Manrope', 'Inter', sans-serif;
  font-size: 42px;
  letter-spacing: -0.06em;
}

.shell-main__header p {
  margin: 10px 0 0;
  max-width: 740px;
  color: var(--text-soft);
  line-height: 1.7;
}

.shell-main__actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.shell-main__search {
  width: 320px;
}

.shell-main__view {
  min-height: 0;
}

@media (max-width: 1280px) {
  .shell-main__header {
    flex-direction: column;
    align-items: stretch;
  }

  .shell-main__actions {
    flex-wrap: wrap;
  }

  .shell-main__search {
    width: 100%;
  }
}

@media (max-width: 900px) {
  .sidebar,
  .shell-main__header {
    border-radius: 24px;
    padding: 18px;
  }

  .shell-main__header h1 {
    font-size: 30px;
  }

  .shell-main__actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
