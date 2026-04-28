<template>
  <button class="glass-button" :class="variantClass" :type="nativeType">
    <slot name="icon" />
    <span><slot /></span>
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = withDefaults(
  defineProps<{
    variant?: 'primary' | 'default' | 'ghost';
    nativeType?: 'button' | 'submit' | 'reset';
  }>(),
  {
    variant: 'default',
    nativeType: 'button',
  },
);

const variantClass = computed(() => {
  if (props.variant === 'primary') {
    return 'glass-button--primary';
  }

  if (props.variant === 'ghost') {
    return 'glass-button--ghost';
  }

  return '';
});
</script>
