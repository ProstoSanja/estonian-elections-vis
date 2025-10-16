import {ref} from 'vue'
import {defineStore} from 'pinia'
import type { Candidate, District, Party, ProcessedResults } from '@/data/api-types'
import axios from 'axios'
import {computed} from 'vue'
import { useElectionName } from './useElectionName'
import { candidateUniqueId } from '@/data/data-lookups'

export const useElectionDataStore = defineStore('electionData', () => {
  const electionData = ref<ProcessedResults | null>(null)
  const lastFetch = ref<Date>(new Date(0))

  const { electionName } = useElectionName()

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
      acc[candidateUniqueId(candidate)] = candidate
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
