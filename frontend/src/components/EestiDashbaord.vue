<script setup lang="ts">
import { useDashboardContentStore } from '@/stores/dashboardContent'
import RegionRace from '@/components/RegionRace.vue'
import { useElectionDataStore } from '@/stores/electionData'
import CandidateCard from '@/components/CandidateCard.vue'

const dashboardContentStore = useDashboardContentStore()
const electionDataStore = useElectionDataStore()

</script>
<template>
  <template v-for="entry in dashboardContentStore.dashboardContent" :key="`${entry.type}-${entry.code}`">
      <RegionRace
        v-if="entry.type === 'region'"
        :regionEHAK="Number(entry.code)"
        :onRemove="() => dashboardContentStore.toggleEntry(entry)"
      />

      <CandidateCard v-if="entry.type === 'candidate' && electionDataStore.candidatesByToken[entry.code]" :candidate="electionDataStore.candidatesByToken[entry.code]!"
        :party="electionDataStore.partiesByCode[electionDataStore.candidatesByToken[entry.code]!.partyCode]"
        :class="'self-stretch'"
        @click="dashboardContentStore.toggleEntry(entry)" />
      <!-- Future: Add party and candidate components here -->
      <!-- <PartyRace v-else-if="entry.type === 'party'" :partyCode="entry.code" /> -->
      <!-- <CandidateRace v-else-if="entry.type === 'candidate'" :candidateCode="entry.code" /> -->
    </template>
</template>
