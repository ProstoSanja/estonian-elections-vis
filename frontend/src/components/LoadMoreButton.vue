<script setup lang="ts">
const emit = defineEmits<{
  (e: 'loadMore'): void
}>()
</script>

<template>
  <div class="flex flex-row items-center justify-center rounded-xl p-4 opacity-70 hover:opacity-100 hover:ring-2 ring-slate-600 transition-opacity cursor-pointer bg-gradient-to-l from-slate-700 to-slate-600" @click="emit('loadMore')">
      <span class="text-xl opacity-80">Laadi rohkem</span>
    </div>
</template>

