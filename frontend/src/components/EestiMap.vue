<!-- eslint-disable @typescript-eslint/no-explicit-any -->
<script setup lang="ts">
import { onMounted, computed, ref } from "vue";
import { registerMap } from "echarts/core";
import VChart from "vue-echarts";
import { useElectionDataStore } from "@/stores/electionData";
import { useDashboardContentStore } from "@/stores/dashboardContent";
import { getPartyColor } from "@/data/data-lookups";
import Color from 'color';
import type { District } from "@/data/api-types";

const electionDataStore = useElectionDataStore()
const dashboardContentStore = useDashboardContentStore()
const mapLoaded = ref(false)

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
    map: "custom",
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
  let mapJson;

  switch (electionName) {
    case 'KOV2021':
      mapJson = await import('@/data/kov2021.geo.json');
      break;
    case 'RK2023':
      mapJson = await import('@/data/rk2023.geo.json');
      break;
    case 'KOV2025':
      mapJson = await import('@/data/kov2025.singleTallinn.geo.json');
      break;
    default:
      console.error(`Unknown election type: ${electionName}`);
      return;
  }

  registerMap("custom", (mapJson.default || mapJson) as any);
  mapLoaded.value = true;
});

const handleMapClick = (params: any) => {
  if (params.data?.electionDistrict) {
    const district = params.data.electionDistrict as District;
    dashboardContentStore.toggleEntry({
      type: 'region',
      code: district.number
    });
  }
};

</script>

<template>
  <div class="flex items-center justify-center max-h-[55vh] !h-[60vw] md:!h-[35vw] w-full">
    <VChart v-if="mapLoaded" :option="option" :autoresize="true" class="max-h-[55vh] !h-[60vw] md:!h-[35vw]" @click="handleMapClick"/>
    <p v-else class="text-gray-500">Kaart on laadimas...</p>
  </div>
</template>
