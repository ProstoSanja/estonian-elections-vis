import {ref, watch} from 'vue'
import {defineStore} from 'pinia'
import { useElectionName } from './useElectionName'
import { usePushNotifications } from './usePushNotifications'

export type DashboardContentEntry = {
  type: 'REGION' | 'CANDIDATE' //| 'party'
  code: number | string
}

export const useDashboardContentStore = defineStore('dashboardContent', () => {
  const { electionName } = useElectionName()
  const pushNotifications = usePushNotifications()

  const dashboardContent = ref<DashboardContentEntry[]>(loadDashboard(electionName.value))

  watch(dashboardContent, (newValue) => {
    saveDashboard(newValue, electionName.value)
    pushNotifications.updateTopics(electionName.value, newValue)
  }, { deep: true, immediate: true })

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

const getStorageKey = (electionName: string) => `dashboardContent_${electionName}`

const typeToPrefix = {
  'REGION': 'r',
  'CANDIDATE': 'c'
} as const

const prefixToType = Object.fromEntries(
  Object.entries(typeToPrefix).map(([type, prefix]) => [prefix, type])
)

const encodeDashboard = (entries: DashboardContentEntry[]): string => {
  const shorthandEncoded = entries.map(entry => {
    const prefix = typeToPrefix[entry.type]
    return `${prefix}:${entry.code}`
  }).join(',')
  return btoa(shorthandEncoded)
}

const decodeDashboard = (encoded: string): DashboardContentEntry[] => {
  if (!encoded) return []

  const shorthandEncoded = atob(encoded)

  return shorthandEncoded.split(',').map(item => {
    const [prefix, code] = item.split(':')
    const type = prefixToType[prefix ?? '']
    return { type, code }
  }).filter((entry) => {
    return entry.code !== undefined && entry.type !== undefined
  }).map((entry) => entry as DashboardContentEntry)
}

const loadDashboard = (electionName: string): DashboardContentEntry[] => {
  // Try to load from URL search query first
  try {
    const urlParams = new URLSearchParams(window.location.search)
    const urlDashboard = urlParams.get('d')
    if (urlDashboard) {
      const decoded = decodeDashboard(urlDashboard)
      if (decoded.length > 0) return decoded
    }
  } catch (error) {
    console.error('Failed to load from URL:', error)
  }

  // Fall back to localStorage
  try {
    const stored = localStorage.getItem(getStorageKey(electionName))
    if (stored) {
      const decoded = decodeDashboard(stored)
      if (decoded.length > 0) return decoded
    }
  } catch (error) {
    console.error('Failed to load from localStorage:', error)
  }

  // Fall back to default
  return [
    {type: 'REGION', code: 0},
    {type: 'REGION', code: 784},
  ]
}

const saveDashboard = (content: DashboardContentEntry[], electionName: string) => {
  try {
    localStorage.setItem(getStorageKey(electionName), encodeDashboard(content))
  } catch (error) {
    console.error('Failed to save to localStorage:', error)
  }
}
