import {ref} from 'vue'
import {defineStore} from 'pinia'
import {computed} from 'vue'

export const useLoadingGatekeeper = defineStore('loadingGatekeeper', () => {
  // loading parts
  const mapLoaded = ref(false)
  const dataLoaded = ref(false)

  // overrides
  const delayedLoading = ref(false)
  const forceLoaded = ref(false)

  setTimeout(() => {
    delayedLoading.value = true
  }, 2000)

  setTimeout(() => {
    forceLoaded.value = true
  }, 10000)

  // final result
  const fullyLoaded = computed(() => forceLoaded.value || (mapLoaded.value && dataLoaded.value))

  const announceLoaded = (type: 'map' | 'data') => {
    if (type === 'map') {
      mapLoaded.value = true
    } else {
      dataLoaded.value = true
    }
  }

  return { delayedLoading,fullyLoaded, announceLoaded, mapLoaded, dataLoaded }
})
