<script setup lang="ts">
import { getPartyColor } from '@/data/data-lookups'
import type { Candidate, Party } from '@/data/api-types'

defineProps<{
  candidate: Candidate
  party: Party | undefined
}>()
</script>

<template>
  <div class="flex flex-row items-stretch rounded-xl p-4 gap-4 hover:opacity-80 transition-opacity cursor-pointer"
    :style="{
      backgroundImage: `linear-gradient(to right, ${getPartyColor(candidate.partyCode)} 0%, ${getPartyColor(candidate.partyCode)} 7px, ${getPartyColor(candidate.partyCode)}60 7px, ${getPartyColor(candidate.partyCode)}20 100%)`
    }">
    <div class="flex flex-col items-start min-w-0 flex-grow">
      <div class="text-sm font-bold overflow-hidden text-ellipsis whitespace-nowrap w-full">
        {{ candidate.forename }} {{ candidate.surename }}
      </div>
      <div class="text-sm overflow-hidden text-ellipsis whitespace-nowrap w-full">{{ party?.name ?? candidate.partyCode }}</div>
    </div>
    <div class="flex flex-col items-end justify-center flex-grow pr-2">
      <div class="text-xl font-bold">{{ candidate.votes }}</div>
    </div>
  </div>
</template>

