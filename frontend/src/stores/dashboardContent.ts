import {ref, watch} from 'vue'
import {defineStore} from 'pinia'

export type DashboardContentEntry = {
  type: 'region' | 'candidate' //| 'party'
  code: number | string
}

export const useDashboardContentStore = defineStore('dashboardContent', () => {
  const dashboardContent = ref<DashboardContentEntry[]>(loadDashboard())

  watch(dashboardContent, (newValue) => {
    saveDashboard(newValue)
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

const STORAGE_KEY = 'dashboardContent'

const typeToPrefix = {
  'region': 'r',
  'candidate': 'c'
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

const loadDashboard = (): DashboardContentEntry[] => {
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
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored) {
      const decoded = decodeDashboard(stored)
      if (decoded.length > 0) return decoded
    }
  } catch (error) {
    console.error('Failed to load from localStorage:', error)
  }

  // Fall back to default
  return [
    {type: 'region', code: 0},
    {type: 'region', code: 784},
  ]
}

const saveDashboard = (content: DashboardContentEntry[]) => {
  try {
    localStorage.setItem(STORAGE_KEY, encodeDashboard(content))
  } catch (error) {
    console.error('Failed to save to localStorage:', error)
  }
}
