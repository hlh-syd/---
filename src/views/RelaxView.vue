<template>
  <div class="page-grid grid-2">
    <SectionCard eyebrow="Fortune" title="看你八字" subtitle="保持轻松模块，但沿用整套玻璃卡与留白体系。">
      <div class="relax-card">
        <div class="relax-card__symbol">命</div>
        <p>{{ store.relaxFortune }}</p>
      </div>
    </SectionCard>

    <SectionCard eyebrow="Food" title="今天吃什么？" subtitle="给出一个即时饮食建议，用作工作台中的情绪缓冲区。">
      <div class="relax-card">
        <div class="relax-card__symbol">食</div>
        <p>{{ store.foodSuggestion }}</p>
      </div>
    </SectionCard>
  </div>
</template>

<script setup lang="ts">
import SectionCard from '@/components/ui/SectionCard.vue';
import { useWorkspaceStore } from '@/stores/workspace';

const store = useWorkspaceStore();
</script>

<style scoped>
.relax-card {
  display: grid;
  gap: 18px;
  min-height: 260px;
  align-content: center;
  justify-items: center;
  text-align: center;
  padding: 24px;
  border-radius: 30px;
  background:
    radial-gradient(circle at top, rgba(66, 123, 255, 0.14), transparent 34%),
    rgba(248, 250, 255, 0.84);
  border: 1px solid rgba(205, 213, 228, 0.72);
}

.relax-card__symbol {
  display: grid;
  place-items: center;
  width: 78px;
  height: 78px;
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(66, 123, 255, 0.18), rgba(25, 183, 160, 0.12));
  font-family: 'Manrope', 'Inter', sans-serif;
  font-size: 32px;
  font-weight: 800;
}

.relax-card p {
  margin: 0;
  max-width: 420px;
  color: var(--text-soft);
  line-height: 1.9;
}
</style>
