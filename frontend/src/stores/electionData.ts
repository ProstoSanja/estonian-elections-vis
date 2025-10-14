import {ref} from 'vue'
import {defineStore} from 'pinia'
import type { ElectionType, ProcessedResults } from '@/data/api-types'
import axios from 'axios'
import {useRoute} from 'vue-router'
import {computed} from 'vue'

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

  return { electionData, fetchElectionData, lastFetch, electionName }
})
