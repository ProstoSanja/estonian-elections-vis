<script setup lang="ts">
import { computed } from 'vue'
import { useElectionDataStore } from '@/stores/electionData'
import { getPartyColor, getShortPartyCode } from '@/data/data-lookups'

const props = defineProps<{
  regionEHAK: number
}>()

const electionDataStore = useElectionDataStore()

const race = computed(() => {
  return electionDataStore.electionData?.districts.find(district => district.number === props.regionEHAK)
})

const voteStatsWithEVotes = computed(() => {
  if (!race.value) return undefined
  const eVotesAsProtocols = race.value.voteStats.protocolsTotal
  const protocolsTotal = race.value.voteStats.protocolsTotal + eVotesAsProtocols
  const protocolsCounted = race.value.voteStats.protocolsCounted + (race.value.voteStats.evotesCounted ? eVotesAsProtocols : 0)
  // const protocolsCounted = race.value.voteStats.protocolsCounted + (false ? eVotesAsProtocols : 0)
  const votesPerProtocol = race.value.voteStats.votesCounted / protocolsCounted
  const votesTotal = votesPerProtocol * protocolsTotal
  const uncountedVotes = votesTotal - race.value.voteStats.votesCounted
  const uncountedVotesShare = uncountedVotes / votesTotal
  return {
    evotesCounted: race.value.voteStats.evotesCounted,
    votesCounted: race.value.voteStats.votesCounted,
    totalMandates: race.value.totalMandates,
    protocolsCounted,
    protocolsTotal,
    votesPerProtocol,
    votesTotal,
    uncountedVotes,
    uncountedVotesShare
  }
})

const partiesWithParticipation = computed(() => {
  if (!race.value || !voteStatsWithEVotes.value) return undefined
  return race.value.parties.map(party => {
    const shareOfCountedVotes = party.votes / race.value!.voteStats.votesCounted
    const shareOfTotalVotes = party.votes / voteStatsWithEVotes.value!.votesTotal
    return {
      ...party,
      color: getPartyColor(party.code),
      shareOfCountedVotes,
      shareOfTotalVotes
    }
  })
})

const shouldShowMandateDistribution = computed(() => {
  return voteStatsWithEVotes.value?.evotesCounted && voteStatsWithEVotes.value?.uncountedVotesShare < 0.4
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
  <div class="flex flex-col gap-4 items-stretch flex-1">
    <h1 class="text-2xl font-bold text-center">{{ race?.name }}</h1>

    <div class="flex flex-col items-stretch flex-1 rounded-xl overflow-clip"
      v-if="voteStatsWithEVotes && partiesWithParticipation">
      <div class="flex flex-row h-10 bg-slate-700" v-if="partiesWithParticipation.length > 0">
        <div v-for="party in partiesWithParticipation" :key="party.code"
          class="h-full hover:opacity-70 transition-opacity flex flex-col justify-center items-center" :style="{
            backgroundColor: party.color,
            marginRight: `${party.shareOfCountedVotes * voteStatsWithEVotes.uncountedVotesShare * 100}%`,
            width: `${party.shareOfTotalVotes * 100}%`
          }">
          <span v-if="party.shareOfTotalVotes > 0.1" class="lg:text-base md:text-sm text-xs font-bold">{{
            getShortPartyCode(party.code, 'very')
          }}</span>
          <span v-else-if="party.shareOfTotalVotes > 0.06" class="lg:text-base md:text-sm text-xs font-bold">{{
            getShortPartyCode(party.code, 'ultra') }}</span>
        </div>
      </div>
      <div class="flex flex-row h-10 bg-slate-700 justify-center items-center" v-else>
        <span class="opacity-70 text-xs lg:text-base overflow-hidden text-ellipsis whitespace-nowrap px-1">
          Ootame andmete laekumist valimiskomisjonist
        </span>
      </div>

      <!-- // mandate distribution -->
      <div class="flex flex-row h-10 bg-slate-700 relative border-t-[0.5px] border-slate-900"
        v-if="shouldShowMandateDistribution">
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
          class="absolute top-0 bottom-0 w-[0.5px] bg-slate-900 opacity-50 z-10" :style="{ left: `${position}%` }">
        </div>
      </div>
      <div v-else class="flex flex-row h-10 bg-slate-700 justify-center items-center border-t-[0.5px] border-slate-900">
        <span class="opacity-70 text-xs lg:text-base overflow-hidden text-ellipsis whitespace-nowrap px-1">Mandaatide
          jaotuse ennustamiseks ootame rohkem andmeid</span>
      </div>
    </div>
    <div v-else class="flex flex-row h-10 rounded-xl bg-slate-700 overflow-clip justify-center items-center">
      <span class="opacity-70">Ootame esimeste protokollide lugemist</span>
    </div>
  </div>
</template>
