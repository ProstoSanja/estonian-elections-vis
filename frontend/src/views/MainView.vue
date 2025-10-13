<script setup lang="ts">
import MapComponent from '@/components/EestiMap.vue'
import RegionRace from '@/components/RegionRace.vue'
import EestiCandidates from '@/components/EestiCandidates.vue'
import {useElectionDataStore} from '@/stores/electionData'
import FooterView from '@/components/FooterView.vue'

import {useRoute} from 'vue-router'
import {onMounted, onUnmounted, ref, computed} from 'vue'
import type { ElectionType } from '@/data/api-types'

const route = useRoute()
const electionDataStore = useElectionDataStore()
const intervalId = ref<number | null>(null)

const electionName = computed(() => {
  const routeName = route.name
  return routeName === 'index' || !routeName ? 'KOV2025' : routeName as ElectionType
})

const electionTitle = computed(() => {
  return {
    type: electionName.value.replace(/\d+$/, ''),
    year: electionName.value.match(/\d+$/)?.[0] || ''
  }
})

onMounted(() => {
  electionDataStore.fetchElectionData(electionName.value)

  intervalId.value = window.setInterval(() => {
    electionDataStore.fetchElectionData(electionName.value)
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
  return voteCount > 0 || (globalRegion?.voteStats.evotesCounted ?? false)
})
</script>
<template>
  <div class="flex flex-col p-4 md:p-8 gap-4 bg-slate-800 text-white min-h-screen items-center">
    <h1 class="text-6xl font-bold">Eesti {{ electionTitle.type }} {{ electionTitle.year }} valimisõhtu</h1>
    <span class="text-xl" v-if="!globalVoteCountingStarter">Ootame esimesi hääli valimispäeval pärast kella 20:00</span>
    <MapComponent />
    <div class="flex flex-col md:flex-row gap-8 items-stretch self-stretch mb-8">
      <RegionRace :regionEHAK="0" />
      <RegionRace :regionEHAK="784" />
    </div>
    <EestiCandidates />
    <FooterView />
  </div>
</template>
