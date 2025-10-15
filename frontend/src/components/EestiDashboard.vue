<script setup lang="ts">
import { useDashboardContentStore } from '@/stores/dashboardContent'
import RegionRace from '@/components/RegionRace.vue'
import { useElectionDataStore } from '@/stores/electionData'
import CandidateCard from '@/components/CandidateCard.vue'
import TutorialCard from '@/components/TutorialCard.vue'

const dashboardContentStore = useDashboardContentStore()
const electionDataStore = useElectionDataStore()

// TODO: Style dashboard and add hints/examples that you can add to it. have a ref (manually changed) that would disable all hints once an update is performed and it is not prefilled form URL or storage.
// TODO: Notifications?
// TODO: Lookup candidates from big map and fallback to small map in case they are missing from RESULTS dump
// TODO: Tallinn districts
// TODO: Detailed views for candidates and parties in sidebar. enabled with toggle
// TODO: Some indicator of how many votes have been counted, for region and for candidates?
// TODO: For candidates add stats from previous elections? Same for parties?
// TODO: Exit polls/predictions for these elections?

</script>
<template>
  <template v-for="entry in dashboardContentStore.dashboardContent" :key="`${entry.type}-${entry.code}`">
    <RegionRace
      v-if="entry.type === 'region'"
      :district="electionDataStore.districtsByNumber[Number(entry.code)]"
      :onRemove="() => dashboardContentStore.toggleEntry(entry)"
    />

    <CandidateCard v-if="entry.type === 'candidate' && electionDataStore.candidatesByToken[entry.code]" :candidate="electionDataStore.candidatesByToken[entry.code]"
      :party="electionDataStore.partiesByCode[electionDataStore.candidatesByToken[entry.code]?.partyCode ?? '']"
      :class="'self-stretch'"
      :onRemove="() => dashboardContentStore.toggleEntry(entry)" />
  </template>
  <TutorialCard />
</template>
