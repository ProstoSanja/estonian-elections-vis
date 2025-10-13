import {ref} from 'vue'
import {defineStore} from 'pinia'
import type { ElectionType, ProcessedResults } from '@/data/api-types'
import axios from 'axios'

export const useElectionDataStore = defineStore('electionData', () => {
  const electionData = ref<ProcessedResults | null>(null)
  const lastFetch = ref<Date>(new Date(0))

  const fetchElectionData = async (electionType: ElectionType): Promise<ProcessedResults> => {
    const response = await axios.get<ProcessedResults>(`/api/data/${electionType}`);
    electionData.value = response.data
    lastFetch.value = new Date()
    return response.data
  };

  return { electionData, fetchElectionData, lastFetch }
})
