<template>
  <div class="page-grid">
    <SectionCard eyebrow="Tools" title="工具列表（外部网页）" subtitle="沿用聊天页的玻璃卡片语言展示外部 AI 工具入口。">
      <div class="page-grid grid-3">
        <article v-for="tool in store.aiTools" :key="tool.id" class="ai-tool">
          <span class="glass-chip">{{ tool.tag }}</span>
          <h3>{{ tool.name }}</h3>
          <p>{{ tool.description }}</p>
          <a class="ai-tool__link" :href="tool.url" rel="noreferrer" target="_blank">打开网页</a>
        </article>
      </div>
    </SectionCard>

    <div class="split-card">
      <SectionCard eyebrow="Prompt Optimize" title="提示词优化" subtitle="把原始需求压缩成稳定、可执行、可追溯的提示。">
        <div class="page-grid">
          <el-input
            v-model="store.promptDraft"
            :autosize="{ minRows: 5, maxRows: 10 }"
            class="app-input"
            placeholder="输入原始提示词"
            type="textarea"
          />
          <div class="mono-block">{{ store.promptOptimized }}</div>
        </div>
      </SectionCard>

      <SectionCard eyebrow="Bio Assistant" title="Bio 生物医学助手" subtitle="保留问答卡片，但把内容收束成面向医学信息学的摘要。">
        <div class="bio-assistant">
          <div class="bio-assistant__answer">{{ store.bioAssistantAnswer }}</div>
          <ul class="list-reset bio-assistant__points">
            <li>优先使用疾病 / 样本 / 方法 / 终点四段式模板。</li>
            <li>所有结论必须绑定来源与实验条件。</li>
            <li>知识库回答优先回引到已上传 PDF 与笔记文件。</li>
          </ul>
        </div>
      </SectionCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import SectionCard from '@/components/ui/SectionCard.vue';
import { useWorkspaceStore } from '@/stores/workspace';

const store = useWorkspaceStore();
</script>

<style scoped>
.ai-tool {
  display: grid;
  gap: 16px;
  padding: 22px;
  border-radius: 28px;
  border: 1px solid rgba(205, 213, 228, 0.72);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(242, 247, 255, 0.72)),
    rgba(255, 255, 255, 0.42);
  backdrop-filter: blur(18px);
}

.ai-tool h3 {
  margin: 0;
  font-size: 24px;
  font-family: 'Manrope', 'Inter', sans-serif;
}

.ai-tool p {
  margin: 0;
  color: var(--text-soft);
  line-height: 1.8;
}

.ai-tool__link {
  color: var(--brand);
  font-weight: 700;
}

.bio-assistant {
  display: grid;
  gap: 18px;
}

.bio-assistant__answer {
  padding: 20px;
  border-radius: 26px;
  background: rgba(248, 250, 255, 0.84);
  border: 1px solid rgba(203, 212, 228, 0.72);
  line-height: 1.85;
}

.bio-assistant__points {
  display: grid;
  gap: 12px;
}

.bio-assistant__points li {
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(213, 220, 235, 0.72);
}
</style>
