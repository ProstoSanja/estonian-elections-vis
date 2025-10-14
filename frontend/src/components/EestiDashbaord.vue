<script setup lang="ts">
import { useDashboardContentStore } from '@/stores/dashboardContent'
import RegionRace from '@/components/RegionRace.vue'
import { useElectionDataStore } from '@/stores/electionData'
import CandidateCard from '@/components/CandidateCard.vue'

const dashboardContentStore = useDashboardContentStore()
const electionDataStore = useElectionDataStore()

// TODO: Save dashboard store to local storage
// TODO: Region filter
// TODO: handle map lookup errors (wrap with a component (i.e. RegionRaceSelector, CandidateCardSelector) that can do a script calculation and takes a map and key as a param?)
// TODO: Style dashboard and add hints/examples that you can add to it. have a ref (manually changed) that would disable all hints once an update is performed and it is not prefilled form URL or storage.
// TODO: Some indicator of how many votes have been counted, for region and for candidates?
// TODO: Tallinn districts
// TODO: For candidates add stats from previous elections? Same for parties?
// TODO: Exit polls/predictions for these elections?
// TODO: Verify that accessing data using maps is reactive

</script>
<template>
  <template v-for="entry in dashboardContentStore.dashboardContent" :key="`${entry.type}-${entry.code}`">
      <RegionRace
        v-if="entry.type === 'region'"
        :district="electionDataStore.districtsByNumber[Number(entry.code)]!"
        :onRemove="() => dashboardContentStore.toggleEntry(entry)"
      />

      <CandidateCard v-if="entry.type === 'candidate' && electionDataStore.candidatesByToken[entry.code]" :candidate="electionDataStore.candidatesByToken[entry.code]!"
        :party="electionDataStore.partiesByCode[electionDataStore.candidatesByToken[entry.code]!.partyCode]"
        :class="'self-stretch'"
        @click="dashboardContentStore.toggleEntry(entry)" />
    </template>
</template>
