<!-- eslint-disable @typescript-eslint/no-explicit-any -->
<script setup lang="ts">
import { onMounted, onUnmounted, computed, ref } from "vue";
import { registerMap } from "echarts/core";
import VChart from "vue-echarts";
import { useElectionDataStore } from "@/stores/useElectionData";
import { useDashboardContentStore } from "@/stores/useDashboardContent";
import { getPartyColor } from "@/data/data-lookups";
import Color from 'color';
import type { District } from "@/data/api-types";
import { BarsArrowDownIcon } from "@heroicons/vue/24/solid";
import { useLoadingGatekeeper } from "@/stores/useLoadingGatekeeper";

const loadingGatekeeper = useLoadingGatekeeper()
const electionDataStore = useElectionDataStore()
const dashboardContentStore = useDashboardContentStore()
const mapType = ref(1)

const mapData = computed(() => {
  if (!electionDataStore.electionData) return [];

  return electionDataStore.electionData.districts.map(district => {
    if (district.parties.length === 0) {
      return {
        name: district.name,
        value: 0,
        electionDistrict: district,
        itemStyle: {
          areaColor: '#cccccc'
        },
        emphasis: {
          itemStyle: {
            areaColor: '#aaaaaa'
          }
        }
      };
    }

    const leadingParty = district.parties.reduce((prev, current) =>
      (current.votes > prev.votes) ? current : prev
    );

    const leadingPartyColor = Color(getPartyColor(leadingParty.code))

    return {
      name: district.name,
      value: leadingParty.votes,
      electionDistrict: district,
      itemStyle: {
        areaColor: leadingPartyColor.hex()
      },
      emphasis: {
        itemStyle: {
          areaColor: leadingPartyColor.darken(0.2).hex()
        }
      }
    };
  });
});

const option = computed(() => ({
  tooltip: {
    trigger: 'item',
    formatter: (params: any) => {
      if (!params.data?.electionDistrict) {
        return null
      }
      const district = params.data.electionDistrict as District
      return `<span style="font-size: 1.5em;">${params.name}</span><br/><div style="margin-top: 0.8em;">` +
        `${district.parties.map(party => `<b>${party.name}:</b> ${party.votes}`).join('<br/>')}</div>`;
    }
  },
  series: [{
    type: "map",
    map: `custom${mapType.value}`,
    aspectScale: 1,
    zoom: 1.2,
    roam: false,
    selectedMode: false,
    layoutSize: '100%',
    layoutAlignment: 'center',
    label: {
      show: false
    },
    emphasis: {
      label: {
        show: false
      },
      itemStyle: {
        areaColor: '#aaaaaa'
      }
    },
    itemStyle: {
      borderColor: "#1d293d",
      borderWidth: 0.5
    },
    data: mapData.value
  }]
}));

onMounted(async () => {
  // Dynamically load the map GeoJSON based on the election type
  const electionName = electionDataStore.electionName;
  let mapChartsJson;

  switch (electionName) {
    case 'KOV2021':
      mapChartsJson = [await import('@/data/kov2021.geo.json')];
      break;
    case 'RK2023':
      mapChartsJson = [await import('@/data/rk2023.geo.json')];
      break;
    case 'KOV2025':
      mapChartsJson = [await import('@/data/kov2025.splitTallinn.geo.json'),
      await import('@/data/kov2025.singleTallinn.geo.json')];
      break;
    default:
      console.error(`Unknown election type: ${electionName}`);
      return;
  }

  mapChartsJson.forEach((mapJson, index) => {
    registerMap(`custom${index + 1}`, (mapJson.default || mapJson) as any);
  });
  loadingGatekeeper.announceLoaded('map')

  // Toggle mapType every 10 seconds
  if (mapChartsJson.length > 1) {
    const intervalId = setInterval(() => {
      mapType.value = mapType.value == 1 ? 2 : 1;
    }, 10_000);
    onUnmounted(() => {
      clearInterval(intervalId);
    });
  }
});

const handleMapClick = (params: any) => {
  if (params.data?.electionDistrict) {
    const district = params.data.electionDistrict as District;
    dashboardContentStore.toggleEntry({
      type: 'REGION',
      code: district.number
    });
  }
};

const selectedDistrict = ref<string | undefined>(undefined)
const handleDistrictChange = (event: any) => {
  const districtNumber = parseInt(event.target.value)
  const district = electionDataStore.electionData?.districts.find(district => district.number === districtNumber)
  if (district) {
    dashboardContentStore.toggleEntry({ type: 'REGION', code: district.number })
  }
  selectedDistrict.value = undefined
}

</script>

<template>
  <div class="flex flex-row relative items-center flex-1 self-stretch md:hidden">
    <BarsArrowDownIcon class="absolute left-3 w-5 h-5 text-slate-400" />
    <select v-model="selectedDistrict"
      class="rounded-xl p-2 pl-10 bg-slate-700 outline-none focus:outline-none focus:ring-2 focus:ring-slate-600 w-full max-w-full appearance-none text-slate-400"
      @change="handleDistrictChange">
      <option :value="undefined">Regioonide valik</option>
      <option v-for="district in electionDataStore.electionData?.districts || []" :value="district.number"
        :key="district.number">{{
          district.name }}</option>
    </select>
  </div>
  <div class="flex items-center justify-center max-h-[55vh] !h-[60vw] md:!h-[35vw] w-full">
    <VChart v-if="loadingGatekeeper.mapLoaded" :option="option" :autoresize="true" class="max-h-[55vh] !h-[60vw] md:!h-[35vw]"
      @click="handleMapClick" />
    <p v-else class="text-slate-400">Kaart on laadimas...</p>
  </div>
</template>
