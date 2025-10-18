<script setup lang="ts">
import { computed, ref } from 'vue'
import { getShortPartyCode, shareToPercentage } from '@/data/data-lookups'
import { XMarkIcon, BarsArrowDownIcon } from '@heroicons/vue/16/solid';
import type { District } from '@/data/api-types';
import { useDistrictRaceData } from '@/composable/useDistrictRaceData'

const props = defineProps<{
  district?: District,
  onRemove?: () => void
}>()

const expanded = ref(false)

const race = computed(() => {
  return props.district
})

const { voteStatsWithEVotes, partiesWithParticipation } = useDistrictRaceData(race)

const shouldShowMandateDistribution = computed(() => {
  return voteStatsWithEVotes.value?.evotesCounted && voteStatsWithEVotes.value?.uncountedVotesShare < 0.5
})

const mandateDividers = computed(() => {
  if (!voteStatsWithEVotes.value || voteStatsWithEVotes.value.totalMandates < 2 || voteStatsWithEVotes.value.totalMandates > 100) return []
  const totalMandates = voteStatsWithEVotes.value.totalMandates
  // Create dividers at each mandate boundary (1/n, 2/n, 3/n, etc.)
  // We create totalMandates - 1 dividers (no divider at 0% or 100%)
  return Array.from({ length: totalMandates - 1 }, (_, i) => {
    const mandateNumber = i + 1
    return (mandateNumber / totalMandates) * 100
  })
})

</script>
<template>
  <div class="flex flex-col gap-1 items-stretch w-full group">
    <div class="flex flex-row gap-4 items-center justify-between md:pl-4 mt-2">
      <span class="text-2xl font-bold whitespace-nowrap overflow-hidden text-ellipsis">{{ race?.name }}</span>
      <span class="flex flex-row items-center gap-4 text-slate-600 md:text-slate-500">
        <span class="mt-[-2px] text-base font-medium hover:text-slate-400 cursor-pointer whitespace-nowrap"
          @click="expanded = !expanded">
          Detailid
          <BarsArrowDownIcon class="w-4 h-4 inline" />
        </span>
        <XMarkIcon v-if="onRemove"
          class="w-6 h-6 cursor-pointer md:hidden group-hover:inline group-focus:inline focus:inline hover:text-slate-400"
          @click="onRemove?.()" />
      </span>
    </div>

    <div class="flex flex-col items-stretch flex-1 rounded-xl overflow-clip">
      <div class="flex flex-row h-10 bg-slate-700" v-if="voteStatsWithEVotes && partiesWithParticipation.length > 0">
        <div v-for="party in partiesWithParticipation" :key="party.code"
          class="h-full hover:opacity-70 transition-opacity flex flex-col justify-center items-center" :style="{
            backgroundColor: party.color,
            marginRight: `${party.shareOfCountedVotes * voteStatsWithEVotes.uncountedVotesShare * 100}%`,
            width: `${party.shareOfTotalVotes * 100}%`
          }">
          <span v-if="party.shareOfTotalVotes > 0.1" class="lg:text-base md:text-sm text-xs font-bold">{{
            getShortPartyCode(party.code, 'very') }}</span>
          <span v-else-if="party.shareOfTotalVotes > 0.06" class="lg:text-base md:text-sm text-xs font-bold">{{
            getShortPartyCode(party.code, 'ultra') }}</span>
        </div>
      </div>
      <div v-else class="flex flex-row h-10 bg-slate-700 justify-center items-center">
        <span class="opacity-70 text-xs lg:text-base overflow-hidden text-ellipsis whitespace-nowrap px-1">
          Ootame andmete laekumist valimiskomisjonist
        </span>
      </div>

      <!-- // mandate distribution -->
      <div class="flex flex-row h-10 bg-slate-700 relative border-t-[0.5px] border-slate-800"
        v-if="shouldShowMandateDistribution && voteStatsWithEVotes && partiesWithParticipation.length > 0">
        <div v-for="party in partiesWithParticipation" :key="party.code"
          class="h-full hover:opacity-70 transition-opacity flex flex-col justify-center items-center" :style="{
            backgroundColor: party.color,
            width: `${party.mandates / voteStatsWithEVotes.totalMandates * 100}%`
          }">
          <span v-if="party.mandates > voteStatsWithEVotes.totalMandates * 0.06"
            class="lg:text-base md:text-sm text-xs font-bold z-20 absolute">{{ party.mandates }}</span>
        </div>
        <!-- eslint-disable vue/no-use-v-if-with-v-for -->
        <div v-if="mandateDividers.length > 0" v-for="(position, index) in mandateDividers" :key="index"
          class="absolute top-0 bottom-0 w-[1px] ml-[-0.5px] md:ml-0 md:w-[0.5px] bg-slate-800 opacity-20 md:opacity-50 z-10"
          :style="{ left: `${position}%` }">
        </div>
      </div>
      <div v-else class="flex flex-row h-10 bg-slate-700 justify-center items-center border-t-[0.5px] border-slate-800">
        <span class="opacity-70 text-xs lg:text-base overflow-hidden text-ellipsis whitespace-nowrap px-1">Mandaatide
          jaotuse ennustamiseks ootame rohkem andmeid</span>
      </div>
    </div>

    <div v-if="voteStatsWithEVotes && expanded"
      class="flex flex-row gap-2 items-center justify-between rounded-xl px-4 py-1 text-slate-300 bg-slate-600" :style="{
        backgroundImage: `linear-gradient(to right, var(--color-slate-600) 0%, var(--color-slate-600) ${shareToPercentage(voteStatsWithEVotes.protocolsCountedShare)}%, var(--color-slate-700) ${shareToPercentage(voteStatsWithEVotes.protocolsCountedShare)}%, var(--color-slate-700) 100%)`
      }">
      <span class="hidden xs:inline">Hääli edastatud</span>
      <span class="inline xs:hidden">Edastatud</span>
      <span class="font-mono text-sm">{{ shareToPercentage(voteStatsWithEVotes.protocolsCountedShare) }}% ({{
        voteStatsWithEVotes.originalProtocolsCounted }}/{{ voteStatsWithEVotes.originalProtocolsTotal }}

      <span class="hidden xs:inline">jaoskonda)</span>
      <span class="inline xs:hidden">jaosk.)</span>
      </span>
    </div>

    <!-- // Detailed graph -->
    <div class="flex flex-col group/detailed"
      v-if="voteStatsWithEVotes && partiesWithParticipation.length > 0 && expanded">
      <div v-for="(party, index) in partiesWithParticipation" :key="party.code"
        class="flex flex-row items-center justify-between rounded-r-xl px-4 py-1 text-slate-300 hover:brightness-125 transition"
        :class="{
          'rounded-tl-xl': index === 0,
          'rounded-bl-xl': index === partiesWithParticipation.length - 1
        }" :style="{
          backgroundImage: `linear-gradient(to right, ${party.color} 0%, ${party.color} 7px, ${party.color}60 7px, ${party.color}30 ${party.shareOfCountedVotes * voteStatsWithEVotes.protocolsCountedShare * 2 * 100}%, ${party.color}00 ${party.shareOfCountedVotes * voteStatsWithEVotes.protocolsCountedShare * 2 * 100}%)`
        }">
        <span class="font-medium group-hover/detailed:hidden">{{ party.code }}</span>
        <span class="font-medium hidden group-hover/detailed:block">{{ party.name }}</span>
        <span class="font-mono text-sm">{{ (party.shareOfCountedVotes * 100).toFixed(2) }}% ({{
          party.votes.toLocaleString() }})</span>
      </div>
    </div>
  </div>
</template>
