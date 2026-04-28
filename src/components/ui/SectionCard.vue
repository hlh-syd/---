<template>
  <section class="section-card">
    <div v-if="title || subtitle || eyebrow || $slots.actions" class="section-head">
      <div>
        <span v-if="eyebrow" class="eyebrow">{{ eyebrow }}</span>
        <h2 v-if="title" class="section-title" :style="{ marginTop: eyebrow ? '16px' : '0' }">{{ title }}</h2>
        <p v-if="subtitle" class="section-subtitle">{{ subtitle }}</p>
      </div>
      <slot name="actions" />
    </div>
    <slot />
  </section>
</template>

<script setup lang="ts">
defineProps<{
  eyebrow?: string;
  title?: string;
  subtitle?: string;
}>();
</script>
