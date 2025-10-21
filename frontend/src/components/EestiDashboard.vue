<script setup lang="ts">
import { useDashboardContentStore } from '@/stores/useDashboardContent'
import RegionRace from '@/components/RegionRace.vue'
import { useElectionDataStore } from '@/stores/useElectionData'
import CandidateCard from '@/components/CandidateCard.vue'
import PushNotificationPrompt from './PushNotificationPrompt.vue'
// import ErrPlayer from './ErrPlayer.vue'

const dashboardContentStore = useDashboardContentStore()
const electionDataStore = useElectionDataStore()

// TODO: For candidates add stats from previous elections? Same for parties?
// TODO: Exit polls/predictions for these elections?

</script>
<template>
  <!-- <ErrPlayer /> -->
  <template v-for="entry in dashboardContentStore.dashboardContent" :key="`${entry.type}-${entry.code}`">
    <RegionRace v-if="entry.type === 'REGION'" :district="electionDataStore.districtsByNumber[Number(entry.code)]"
      :onRemove="() => dashboardContentStore.toggleEntry(entry)" />

    <CandidateCard v-if="entry.type === 'CANDIDATE' && electionDataStore.candidatesByToken[entry.code]"
      :candidate="electionDataStore.candidatesByToken[entry.code]" :class="'self-stretch'"
      :onRemove="() => dashboardContentStore.toggleEntry(entry)" />
  </template>
  <PushNotificationPrompt />
</template>
