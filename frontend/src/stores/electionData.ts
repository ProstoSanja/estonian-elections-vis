import {ref} from 'vue'
import {defineStore} from 'pinia'
import type { Candidate, District, ElectionType, Party, ProcessedResults } from '@/data/api-types'
import axios from 'axios'
import {useRoute} from 'vue-router'
import {computed} from 'vue'
import { tokenizeString } from '@/data/search'

export const useElectionDataStore = defineStore('electionData', () => {
  const electionData = ref<ProcessedResults | null>(null)
  const lastFetch = ref<Date>(new Date(0))

  const route = useRoute()
  const electionName = computed(() => {
    const routeName = route.name
    return routeName === 'index' || !routeName ? 'KOV2025' : routeName as ElectionType
  })

  const fetchElectionData = async (): Promise<ProcessedResults> => {
    const response = await axios.get<ProcessedResults>(`/api/data/${electionName.value}`);
    electionData.value = response.data
    lastFetch.value = new Date()
    return response.data
  };

  const partiesByCode = computed(() => {
    return electionData.value?.parties.reduce((acc, party) => {
      acc[party.code] = party
      return acc
    }, {} as Record<string, Party>) || {}
  })

  const candidatesByToken = computed(() => {
    return electionData.value?.candidates.reduce((acc, candidate) => {
      acc[tokenizeString(candidate.forename + candidate.surename + candidate.regNumber.toString())] = candidate
      return acc
    }, {} as Record<string, Candidate>) || {}
  })

  const districtsByNumber = computed(() => {
    return electionData.value?.districts.reduce((acc, district) => {
      acc[district.number] = district
      return acc
    }, {} as Record<number, District>) || {}
  })

  return { electionData, fetchElectionData, lastFetch, electionName, partiesByCode, candidatesByToken, districtsByNumber }
})
