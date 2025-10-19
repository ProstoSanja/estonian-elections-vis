<script setup lang="ts">
import { ref } from 'vue'
import { BarsArrowDownIcon } from '@heroicons/vue/16/solid';

const expanded = ref(false)

</script>
<template>
  <div class="flex flex-col gap-1 items-stretch w-full group">
    <div class="flex flex-row gap-4 items-center justify-between md:pl-4 mt-2">
      <span class="text-2xl font-bold whitespace-nowrap overflow-hidden text-ellipsis">ERR otseülekanne</span>
      <span class="flex flex-row items-center gap-4 text-slate-600 md:text-slate-500">
        <span class="mt-[-2px] text-base font-medium hover:text-slate-400 cursor-pointer whitespace-nowrap"
          @click="expanded = !expanded">
          {{ expanded ? 'Peida' : 'Näita' }}
          <BarsArrowDownIcon class="w-4 h-4 inline" />
        </span>
      </span>
    </div>

    <div v-if="expanded" class="w-full aspect-[16/9] overflow-hidden rounded-xl">
      <iframe src="https://otse.err.ee/k/etv" class="w-full h-[calc(100%+50px)] mt-[-60px] overflow-hidden rounded-xl"></iframe>
    </div>
  </div>
</template>
