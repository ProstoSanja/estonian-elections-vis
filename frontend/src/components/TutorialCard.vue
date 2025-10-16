<script setup lang="ts">
import { CursorArrowRaysIcon } from '@heroicons/vue/24/solid'
import PushNotificationPrompt from '@/components/PushNotificationPrompt.vue'
import { usePushNotifications } from '@/stores/usePushNotifications';

const pushNotifications = usePushNotifications();
</script>

<template>
  <div class="relative flex flex-col items-center self-stretch rounded-xl">
    <svg class="dashed-border-svg" xmlns="http://www.w3.org/2000/svg">
      <rect class="dashed-rect" :class="{ 'pink-stroke': pushNotifications.shouldPrompt, 'green-stroke': pushNotifications.isSubscribed, 'gray-stroke': !pushNotifications.shouldPrompt && !pushNotifications.isSubscribed }"/>
    </svg>

    <PushNotificationPrompt />
    <div class="flex flex-row items-center gap-4 self-stretch p-4 pt-3 text-slate-500 hover:text-slate-400 transition-colors">
      <CursorArrowRaysIcon class="min-w-6 h-6 max-md:hidden" />
      <span class="text-base md:hidden">Valige regiooni või vajutage kandidaadi nimele, et lisada neid siin jälgimiseks ja kuvada rohkem infot.</span>
      <span class="text-base max-md:hidden">Vajutage kaardi regioonile või kandidaadi nimele, et lisada neid siin jälgimiseks ja kuvada rohkem infot.</span>
    </div>
  </div>
</template>
<style scoped>
.dashed-border-svg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  overflow: visible;
}

.dashed-rect {
  fill: none;
  stroke: #ccc;
  stroke-width: 2;
  stroke-dasharray: 15 10; /* dash length, gap length */
  width: 100%;
  height: 100%;
  rx: 12px; /* matches rounded-xl (0.75rem = 12px) */
  ry: 12px;
  x: 1;
  y: 1;
  /* Adjust positioning to account for stroke width */
  vector-effect: non-scaling-stroke;
}

.pink-stroke {
  stroke: #974e7c;
}
.green-stroke {
  stroke: #2b544a;
}
.gray-stroke {
  stroke: var(--color-slate-600);
}
</style>
