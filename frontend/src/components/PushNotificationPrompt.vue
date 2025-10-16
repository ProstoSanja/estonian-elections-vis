<script setup lang="ts">
import { usePushNotifications } from '@/stores/usePushNotifications';
import { ref } from 'vue';
import { BellAlertIcon, CheckBadgeIcon } from '@heroicons/vue/24/solid'

const {
  isSupported,
  isSubscribed,
  error,
  shouldPrompt,
  subscribe,
} = usePushNotifications();

const isLoading = ref(false);

const handleSubscribe = async () => {
  if (isLoading.value) return;
  isLoading.value = true;
  await subscribe();
  isLoading.value = false;
};

const getBackgroundImage = (isSubscribed: boolean) => {
  if (isSubscribed) {
    return `linear-gradient(to right, #2b544a 0%, #2b544a70 100%)`;
  }
  return `linear-gradient(to right, #974e7c 0%, #974e7c70 100%)`;
}

</script>

<template>
  <div v-if="isSupported" class="flex flex-col self-stretch rounded-xl overflow-hidden p-2 hover:opacity-70 transition-opacity" :style="{
    backgroundImage: getBackgroundImage(isSubscribed)
  }">
    <!-- Prompt to enable notifications -->
    <div v-if="shouldPrompt" class="flex flex-row items-center gap-4 p-2 cursor-pointer" @click="handleSubscribe">
      <BellAlertIcon class="min-w-6 h-6 text-slate-300" />
      <div class="flex flex-col items-stretch justify-start">
        <p v-if="!isLoading" class="text-slate-300 text-sm">
          <span class="font-bold">Lülitades sisse teavitusi </span>
          saad kohe teada häälte laekumisest, ning ei ole vaja lõpmatuseni lehte värskendada.
          <span class="font-bold">Vajutage siia et jätkata.</span>
        </p>
        <p v-if="isLoading" class="text-slate-300 text-sm">Palun oodake...</p>
        <p v-if="error" class="text-red-200 text-xs">Tekkis viga: {{ error }}</p>
      </div>
    </div>

    <!-- Success message -->
    <div v-if="isSubscribed" class="flex items-center gap-3 text-green-200 px-2">
      <CheckBadgeIcon class="min-w-6 h-6 text-green-200" />
      <div>
        <p class="">Teavitused sisse lülitatud</p>
      </div>
    </div>
  </div>
</template>
