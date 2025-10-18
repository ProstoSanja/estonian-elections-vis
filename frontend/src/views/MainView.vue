<script setup lang="ts">
import MapComponent from '@/components/EestiMap.vue'
import EestiDashboard from '@/components/EestiDashboard.vue'
import EestiCandidates from '@/components/EestiCandidates.vue'
import { useElectionDataStore } from '@/stores/electionData'
import FooterView from '@/components/FooterView.vue'
import { onMounted, onUnmounted, ref, computed } from 'vue'
import { useLoadingGatekeeper } from '@/stores/useLoadingGatekeeper'

const loadingGatekeeper = useLoadingGatekeeper()
const electionDataStore = useElectionDataStore()
const timeoutId = ref<number | null>(null)

const electionTitle = computed(() => {
  return {
    type: electionDataStore.electionName.replace(/\d+$/, ''),
    year: electionDataStore.electionName.match(/\d+$/)?.[0] || ''
  }
})

const refreshData = async () => {
  if (timeoutId.value !== null) {
    clearTimeout(timeoutId.value)
  }
  try {
    await electionDataStore.fetchElectionData()
    setTimeout(refreshData, 10 * 1000)
  } catch {
    setTimeout(refreshData, 3 * 1000)
  }
}

onMounted(async () => {
  await electionDataStore.fetchElectionData()
  refreshData()
})

onUnmounted(() => {
  if (timeoutId.value !== null) {
    clearTimeout(timeoutId.value)
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
  <div class="flex flex-col md:flex-row w-screen  min-w-screen max-w-screen md:h-screen min-h-screen md:max-h-screen bg-slate-800 text-white md:overflow-hidden scrollbar-thumb-slate-900  scrollbar-track-slate-800 scrollbar-w-2">
    <div class="flex flex-col w-full md:w-1/2 lg:w-3/5 p-4 max-md:pt-2 lg:p-8 gap-4 md:in-h-screen items-center md:overflow-y-auto overflow-x-hidden order-2 md:order-1 md:scrollbar-thin">
      <h1 class="text-6xl font-bold max-md:hidden">Eesti {{ electionTitle.type }} {{ electionTitle.year }} valimisõhtu</h1>
      <span class="text-xl max-md:hidden" v-if="!globalVoteCountingStarter">Häälte lugemine algab valimispäeval kell 20:00</span>
      <MapComponent />
      <EestiCandidates />
      <FooterView />
    </div>
    <div class="flex flex-col w-full md:w-1/2 lg:w-2/5 p-4 max-md:pb-2 lg:p-8 gap-4 md:in-h-screen items-center md:overflow-y-auto overflow-x-hidden md:border-l-2 order-1 md:order-2 md:border-slate-900 md:scrollbar-thin">
      <h1 class="text-5xl font-bold block md:hidden pt-4">Eesti {{ electionTitle.type }} {{ electionTitle.year }} valimisõhtu</h1>
      <!-- <span class="text-xl md:hidden text-slate-400" v-if="!globalVoteCountingStarter">Häälte lugemine algab valimispäeval kell 20:00</span> -->
      <EestiDashboard />
    </div>
  </div>
  <div v-if="!loadingGatekeeper.fullyLoaded" class="fixed bottom-0 left-0 right-0 p-4 top-0 bg-slate-800 flex flex-col items-center justify-center gap-4 text-slate-300">
    <span v-if="loadingGatekeeper.delayedLoading" class="text-3xl">Palun oodake</span>
    <span v-if="loadingGatekeeper.delayedLoading" class="animate-pulse text-xl">Andmeid laaditakse...</span>
  </div>
</template>
