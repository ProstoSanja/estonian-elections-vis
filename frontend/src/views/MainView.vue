<script setup lang="ts">
import MapComponent from '@/components/EestiMap.vue'
import EestiDashbaord from '@/components/EestiDashbaord.vue'
import EestiCandidates from '@/components/EestiCandidates.vue'
import { useElectionDataStore } from '@/stores/electionData'
import FooterView from '@/components/FooterView.vue'
import { onMounted, onUnmounted, ref, computed } from 'vue'

const electionDataStore = useElectionDataStore()
const intervalId = ref<number | null>(null)

const electionTitle = computed(() => {
  return {
    type: electionDataStore.electionName.replace(/\d+$/, ''),
    year: electionDataStore.electionName.match(/\d+$/)?.[0] || ''
  }
})

onMounted(() => {
  electionDataStore.fetchElectionData()

  intervalId.value = window.setInterval(() => {
    electionDataStore.fetchElectionData()
  }, 15 * 1000)
})

onUnmounted(() => {
  if (intervalId.value !== null) {
    clearInterval(intervalId.value)
  }
})

const globalVoteCountingStarter = computed<boolean>(() => {
  const globalRegion = electionDataStore.electionData?.districts.find(district => district.number === 0)
  const voteCount = globalRegion?.voteStats.protocolsCounted ?? 0
  const eVotesCounted = globalRegion?.voteStats.evotesCounted ?? false
  return voteCount > 0 || eVotesCounted
})
</script>
<template>
  <div class="flex flex-col md:flex-row w-screen  min-w-screen max-w-screen md:h-screen min-h-screen md:max-h-screen bg-slate-800 text-white md:overflow-hidden">
    <div class="flex flex-col w-full md:w-1/2 lg:w-3/5 p-4 lg:p-8 gap-4 md:in-h-screen items-center md:overflow-y-auto overflow-x-hidden order-2 md:order-1">
      <h1 class="text-6xl font-bold hidden md:block">Eesti {{ electionTitle.type }} {{ electionTitle.year }} valimisõhtu</h1>
      <span class="text-xl" v-if="!globalVoteCountingStarter">Ootame esimesi hääli valimispäeval alates kella
        20:00</span>
      <MapComponent />
      <EestiCandidates />
      <FooterView />
    </div>
    <div class="flex flex-col w-full md:w-1/2 lg:w-2/5 p-4 lg:p-8 gap-4 md:in-h-screen items-center md:overflow-y-auto overflow-x-hidden md:border-l-2 order-1 md:order-2 md:border-slate-900">
      <h1 class="text-5xl font-bold block md:hidden py-4">Eesti {{ electionTitle.type }} {{ electionTitle.year }} valimisõhtu</h1>
      <EestiDashbaord />
    </div>
  </div>
</template>
