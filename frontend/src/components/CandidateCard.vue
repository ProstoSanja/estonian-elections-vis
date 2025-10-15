<script setup lang="ts">
import { getPartyColor } from '@/data/data-lookups'
import type { Candidate, Party } from '@/data/api-types'
import { XMarkIcon } from '@heroicons/vue/16/solid'

defineProps<{
  candidate?: Candidate
  party?: Party
  onRemove?: () => void
}>()
</script>

<template>
  <div v-if="candidate"
    class="flex flex-row items-center rounded-xl p-4 gap-4 hover:opacity-80 transition-opacity cursor-pointer group"
    :style="{
      backgroundImage: `linear-gradient(to right, ${getPartyColor(candidate.partyCode)} 0%, ${getPartyColor(candidate.partyCode)} 7px, ${getPartyColor(candidate.partyCode)}60 7px, ${getPartyColor(candidate.partyCode)}20 100%)`
    }" @click="onRemove?.()">
    <div class="flex flex-col items-start min-w-0 flex-grow">
      <div class="text-sm font-bold overflow-hidden text-ellipsis whitespace-nowrap w-full">
        {{ candidate.forename }} {{ candidate.surename }}
      </div>
      <div class="text-sm overflow-hidden text-ellipsis whitespace-nowrap w-full">{{ party?.name ?? candidate.partyCode
        }}</div>
    </div>
    <div class="flex flex-col items-end justify-center flex-grow pr-2">
      <div class="text-xl font-bold">{{ candidate.votes }}</div>
    </div>
    <XMarkIcon v-if="onRemove"
      class="w-6 h-6 text-slate-600 md:text-slate-500 cursor-pointer hidden group-hover:block group-focus:block focus:block hover:text-slate-400"
      @click="onRemove?.()" />
  </div>
</template>
