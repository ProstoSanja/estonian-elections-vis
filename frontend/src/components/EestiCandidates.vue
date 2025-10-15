<script setup lang="ts">
import { useElectionDataStore } from '@/stores/electionData'
import { ref, computed } from 'vue'
import CandidateCard from './CandidateCard.vue'
import LoadMoreButton from './LoadMoreButton.vue'
import { MagnifyingGlassIcon } from '@heroicons/vue/24/solid'
import { XMarkIcon } from '@heroicons/vue/16/solid'
import { tokenizeString } from '@/data/search'
import { useDashboardContentStore } from '@/stores/dashboardContent'

const electionDataStore = useElectionDataStore()
const dashboardContentStore = useDashboardContentStore()

const candidateCount = ref(23)
const candidateSearch = ref('')
const partySearch = ref('')
const districtSearch = ref<number | undefined>(undefined)

const candidates = computed(() => {
  const searchToken = tokenizeString(candidateSearch.value)
  return electionDataStore.electionData?.candidates
    .filter(candidate => {
      if (districtSearch.value !== undefined) return candidate.districtNumber === districtSearch.value
      return true
    })
    .filter(candidate => {
      if (partySearch.value) return candidate.partyCode === partySearch.value
      return true
    })
    .filter(candidate => {
      if (!searchToken) return true
      const candidateSearchToken = tokenizeString(candidate.forename + candidate.surename + candidate.regNumber.toString())
      return candidateSearchToken.includes(searchToken)
    })
    .slice(0, candidateCount.value)
})

// TODO: temp for KOV, since parent districts do not work, because candidate numbers are only assigned to children districts
const searchableDistricts = computed(() => {
  return electionDataStore.electionData?.districts.filter(district => {
    return !district.name.toLowerCase().includes('maakond') && !district.name.toLowerCase().includes('vabariik')
  })
})

</script>
<template>
  <div class="flex flex-col gap-8 items-stretch self-stretch">
    <div class="flex flex-col lg:flex-row lg:flex-wrap items-stretch justify-center gap-4">
      <h1 class="text-2xl font-bold text-center lg:w-full">Kandidaadid</h1>
      <div class="flex flex-row flex-1 relative items-center justify-end">
        <div class="flex flex-row relative items-center flex-1 justify-end">
          <MagnifyingGlassIcon class="absolute left-3 w-5 h-5 text-slate-400" />
          <select
            class="rounded-xl p-2 pl-10 bg-slate-700 outline-none focus:outline-none focus:ring-2 focus:ring-slate-600 w-full max-w-full appearance-none"
            :class="{ 'text-slate-400': districtSearch === undefined }" v-model="districtSearch">
            <option :value="undefined">Kõik regioonid</option>
            <option v-for="district in searchableDistricts" :value="district.number" :key="district.number">{{
              district.name }}</option>
          </select>
          <XMarkIcon class="absolute right-3 w-6 h-6 text-slate-400 cursor-pointer" @click.stop="districtSearch = undefined"
            v-if="districtSearch !== undefined" />
        </div>
      </div>
      <div class="flex flex-row flex-1 relative items-center justify-end">
        <div class="flex flex-row relative items-center flex-1 justify-end">
          <MagnifyingGlassIcon class="absolute left-3 w-5 h-5 text-slate-400" />
          <select
            class="rounded-xl p-2 pl-10 bg-slate-700 outline-none focus:outline-none focus:ring-2 focus:ring-slate-600 w-full max-w-full appearance-none"
            :class="{ 'text-slate-400': partySearch.length <= 0 }" v-model="partySearch">
            <option value="">Kõik erakonnad</option>
            <option v-for="party in electionDataStore.electionData?.parties" :value="party.code" :key="party.code">{{
              party.name }}</option>
          </select>
          <XMarkIcon class="absolute right-3 w-6 h-6 text-slate-400 cursor-pointer" @click.stop="partySearch = ''"
            v-if="partySearch.length > 0" />
        </div>
      </div>
      <div class="flex items-center flex-1">
        <div class="flex flex-row relative items-center flex-1 ">
          <MagnifyingGlassIcon class="absolute left-3 w-5 h-5 text-slate-400" />
          <input
            class="rounded-xl p-2 pl-10 bg-slate-700 outline-none focus:outline-none focus:ring-2 focus:ring-slate-600 w-full max-w-full"
            type="text" v-model="candidateSearch" placeholder="Kandidaadi nimi või number" />
          <XMarkIcon class="absolute right-3 w-6 h-6 text-slate-400 cursor-pointer" @click.stop="candidateSearch = ''"
            v-if="candidateSearch.length > 0" />
        </div>
      </div>
    </div>
    <div class="grid grid-cols-[repeat(auto-fill,minmax(min(16rem,100%),1fr))] gap-4"
      :key="`candidates-${candidateSearch}-${partySearch}`">
      <CandidateCard v-for="candidate in candidates" :key="candidate.regNumber" :candidate="candidate"
        :party="electionDataStore.electionData?.parties.find(party => party.code === candidate.partyCode)"
        @click="dashboardContentStore.toggleEntry({ type: 'candidate', code: tokenizeString(candidate.forename + candidate.surename + candidate.regNumber.toString()) })" />
      <LoadMoreButton v-if="(candidates?.length ?? 0) >= candidateCount" @loadMore="candidateCount += 25" />
    </div>
  </div>
</template>
