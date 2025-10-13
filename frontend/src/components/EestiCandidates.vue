<script setup lang="ts">
import { useElectionDataStore } from '@/stores/electionData'
import { ref, computed } from 'vue'
import CandidateCard from './CandidateCard.vue'
import LoadMoreButton from './LoadMoreButton.vue'
import { MagnifyingGlassIcon } from '@heroicons/vue/24/solid'
import { XMarkIcon } from '@heroicons/vue/16/solid'
import { tokenizeString } from '@/data/search'

const electionDataStore = useElectionDataStore()

const candidateCount = ref(23)
const candidateSearch = ref('')

const candidates = computed(() => {
  const searchToken = tokenizeString(candidateSearch.value)
  return electionDataStore.electionData?.candidates
    .filter(candidate => {
      if (!searchToken) return true
      const candidateSearchToken = tokenizeString(candidate.forename + candidate.surename + candidate.regNumber.toString())
      return candidateSearchToken.includes(searchToken)
    })
    .slice(0, candidateCount.value)
})

</script>
<template>
  <div class="flex flex-col gap-8 items-stretch self-stretch">
    <div class="flex md:flex-row flex-col items-stretch justify-center gap-8">
      <div class="md:flex-1"></div>
      <h1 class="text-2xl font-bold text-center">Kandidaadid</h1>
      <div class="flex items-center flex-1">
        <div class="flex flex-row relative items-center flex-1 md:flex-0">
          <MagnifyingGlassIcon class="absolute left-3 w-5 h-5 text-slate-400" />
          <input
            class="rounded-xl p-2 px-10 bg-slate-700 outline-none focus:outline-none focus:ring-2 focus:ring-slate-600 w-full md:w-70 max-w-full"
            type="text" v-model="candidateSearch" placeholder="Kandidaadi nimi või number" />
          <XMarkIcon class="absolute right-3 w-6 h-6 text-slate-400 cursor-pointer" @click="candidateSearch = ''" v-if="candidateSearch.length > 0" />
        </div>
      </div>
    </div>
    <div class="grid grid-cols-[repeat(auto-fill,minmax(18rem,1fr))] gap-4" :key="`candidates-${candidateSearch}`">
      <CandidateCard v-for="candidate in candidates" :key="candidate.regNumber" :candidate="candidate"
        :party="electionDataStore.electionData?.parties.find(party => party.code === candidate.partyCode)" />
      <LoadMoreButton v-if="(candidates?.length ?? 0) >= candidateCount" @loadMore="candidateCount += 25" />
    </div>
  </div>
</template>
