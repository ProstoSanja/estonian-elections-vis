import {ref} from 'vue'
import {defineStore} from 'pinia'

export type DashboardContentEntry = {
  type: 'region' | 'candidate' //| 'party'
  code: number | string
}

export const useDashboardContentStore = defineStore('dashboardContent', () => {
  const dashboardContent = ref<DashboardContentEntry[]>([
    {type: 'region', code: 0},
    {type: 'region', code: 784},
  ])

  const toggleEntry = (entry: DashboardContentEntry) => {
    const dashboardEntryIndex = dashboardContent.value.findIndex(e => e.code === entry.code && e.type === entry.type)
    if (dashboardEntryIndex !== -1) {
      dashboardContent.value.splice(dashboardEntryIndex, 1)
    } else {
      dashboardContent.value.push(entry)
    }
  }

  return { dashboardContent, toggleEntry }
})
