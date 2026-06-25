<template>
  <span class="player-avatar-fallback">
    <img v-if="currentSrc" :src="currentSrc" :alt="alt" @error="tryNextSource" />
    <span v-else>{{ initials }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { resolveMediaUrl } from '../lib/api';

const props = defineProps<{
  sources: Array<string | null | undefined>;
  initials: string;
  alt?: string;
}>();

const sourceIndex = ref(0);

const normalizedSources = computed(() => {
  const seen = new Set<string>();
  return props.sources
    .map((source) => resolveMediaUrl(source))
    .filter((source) => {
      if (!source || seen.has(source)) {
        return false;
      }
      seen.add(source);
      return true;
    });
});

const currentSrc = computed(() => normalizedSources.value[sourceIndex.value] ?? '');

watch(
  () => normalizedSources.value.join('|'),
  () => {
    sourceIndex.value = 0;
  }
);

function tryNextSource() {
  sourceIndex.value += 1;
}
</script>
