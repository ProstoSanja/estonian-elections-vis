import { computed } from 'vue'
import { useRoute } from 'vue-router'
import type { ElectionType } from '@/data/api-types'

export const useElectionName = () => {
  const route = useRoute()

  const electionName = computed(() => {
    const routeName = route.name
    return routeName === 'index' || !routeName ? 'KOV2025' : routeName as ElectionType
  })

  return { electionName }
}

