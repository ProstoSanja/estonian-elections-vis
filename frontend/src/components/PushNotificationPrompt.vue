<script setup lang="ts">
import { usePushNotifications } from '@/stores/usePushNotifications';
import { useDashboardContentStore } from '@/stores/useDashboardContent';
import { useElectionName } from '@/composable/useElectionName';
import { ref, computed } from 'vue';
import { BellAlertIcon, CheckBadgeIcon, CursorArrowRaysIcon, ArrowTurnLeftDownIcon, ArrowUpOnSquareIcon, FolderPlusIcon, CheckIcon } from '@heroicons/vue/24/solid'
import Bowser from 'bowser';

const pushNotifications = usePushNotifications();
const dashboardStore = useDashboardContentStore();
const { electionName } = useElectionName();

const isLoading = ref(false);
const specialOnboardingInProgress = ref(false);

const handleSubscribe = async () => {
  if (shouldProposeSpecialOnboarding.value) {
    specialOnboardingInProgress.value = true;
    return;
  }
  if (isLoading.value) return;
  isLoading.value = true;
  await pushNotifications.subscribe(electionName.value, dashboardStore.dashboardContent);
  isLoading.value = false;
};

const getBackgroundImage = (isSubscribed: boolean) => {
  if (isSubscribed) {
    return `linear-gradient(to right, #2b544a 0%, #2b544a70 100%)`;
  }
  return `linear-gradient(to right, #974e7c 0%, #974e7c70 100%)`;
}

// Special onboarding for in-browser-ios

const browserVersion = computed(() => Bowser.parse(window.navigator.userAgent))

const specialOnboardingType = computed<'iosPre26' | 'ios26Safari' | 'ios26Other' | null>(() => {
  const versionsGuess = [browserVersion.value.browser.version, browserVersion.value.os.version]
  const isIos = browserVersion.value.os?.name?.toLowerCase() === 'ios'
  if (!isIos) {
    return null
  }
  const isIos26Plus = versionsGuess.some(version => version?.includes('26'))
  if (!isIos26Plus) {
    return 'iosPre26'
  }
  const isSafari = browserVersion.value.browser?.name?.toLowerCase() === 'safari'
  return isSafari ? 'ios26Safari' : 'ios26Other'
})

const shouldProposeSpecialOnboarding = computed(() => {
  return !pushNotifications.isSupported && specialOnboardingType.value !== null
})

const onboardingSteps = computed(() => {
  return [
    { enabledFor: ['ios26Safari'], textBefore: 'Vajutage', badge: { text: '...' }, textAfter: 'ekraani alumises paremas nurgas' },
    { enabledFor: ['ios26Safari'], textBefore: 'Vajutage', badge: { icon: 'share', text: 'Share' }, textAfter: 'nuppu' },
    { enabledFor: ['ios26Other', 'iosPre26'], textBefore: 'Vajutage', badge: { icon: 'share', text: 'Share' }, textAfter: 'nuppu aadressiriba juures' },
    { enabledFor: ['ios26Safari', 'ios26Other'], textBefore: 'Uusesti valige', badge: { text: '...' }, textAfter: 'dialoogi alumises paremas nurgas' },
    { enabledFor: ['ios26Safari', 'ios26Other', 'iosPre26'], textBefore: 'Kerige alla ja vajutage', badge: { icon: 'folder', text: 'Add to Home Screen' }, textAfter: 'nuppu' },
    { enabledFor: ['ios26Safari', 'ios26Other', 'iosPre26'], textBefore: 'Pärast seda saate avada veebilehe koos teavituste toega iPhone koduekraanilt!' }
  ]
    .filter(step => step.enabledFor.includes(specialOnboardingType.value ?? ''))
})

// Special onboarding for running as PWA on iOS (auto-enable notifications)

const isRunningAsPWA = computed(() => {
  const isStandalone = window.matchMedia('(display-mode: standalone)').matches
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const isIOSStandalone = (window.navigator as any).standalone === true
  return isStandalone || isIOSStandalone
})

</script>

<template>
  <div class="flex flex-col self-stretch rounded-xl p-0.5" :style="{
    backgroundImage: getBackgroundImage(pushNotifications.isSubscribed)
  }">
    <div v-if="specialOnboardingInProgress" class="flex flex-col gap-4 p-2 pl-4 pr-3">
      <span>iPhone kasutajatele on vaja teha paar lisa sammu Apple platformi piirangute tõttu. <span
          class="font-semibold">See võtab 30 sekundit.</span></span>

      <div class="flex flex-col gap-3 text-slate-200">
        <div v-for="(step, index) in onboardingSteps" :key="index" class="flex flex-row gap-3 items-center">
          <span
            class="flex-shrink-0 w-6 h-6 rounded-full bg-slate-700 flex items-center justify-center text-sm font-semibold">
            <CheckIcon v-if="index === onboardingSteps.length - 1" class="w-4 h-4 inline" />
            <template v-else>{{ index + 1 }}</template>
          </span>
          <span class="">
            {{ step.textBefore }}
            <span v-if="step.badge" class="bg-slate-700 rounded-md py-1 px-2 whitespace-nowrap">
              <ArrowUpOnSquareIcon v-if="step.badge.icon === 'share'" class="w-4 h-4 inline mt-[-4px]" />
              <FolderPlusIcon v-else-if="step.badge.icon === 'folder'" class="w-4 h-4 inline mt-[-4px]" />
              {{ step.badge.text }}
            </span>
            {{ step.textAfter }}
          </span>
        </div>
      </div>
    </div>
    <!-- Prompt to enable notifications -->
    <div v-else-if="pushNotifications.shouldPrompt || shouldProposeSpecialOnboarding"
      class="flex flex-row items-center gap-4 p-2 pl-4 pr-3 cursor-pointer hover:brightness-125 transition"
      @click="handleSubscribe">
      <BellAlertIcon class="min-w-6 h-6 text-slate-300" />
      <div class="flex flex-col items-stretch justify-start">
        <p v-if="isRunningAsPWA && !isLoading" class="text-slate-300 text-sm">
          <span class="font-bold">Vajutage siia et lõpetada teavituste sätestamist!</span>
        </p>
        <p v-else-if="!isLoading" class="text-slate-300 text-sm">
          <span class="font-bold">Lülitades sisse teavitusi </span>
          saad kohe teada häälte laekumisest, ning ei ole vaja lõpmatuseni lehte värskendada.
          <span class="font-bold">Vajutage siia et jätkata.</span>
        </p>
        <p v-else-if="isLoading" class="text-slate-300 text-sm">Palun oodake...</p>
        <p v-if="pushNotifications.error" class="text-red-200 text-xs">Tekkis viga: {{ pushNotifications.error }}</p>
      </div>
    </div>

    <!-- Success message -->
    <div v-else-if="pushNotifications.isSubscribed"
      class="flex items-center gap-4 text-green-200 p-2 pl-4 pr-3 hover:brightness-125 transition">
      <CheckBadgeIcon class="min-w-6 h-6 text-green-200" />
      <div>
        <p class="">Teavitused sisse lülitatud</p>
      </div>
    </div>

    <!-- Tutorial -->
    <div v-if="!specialOnboardingInProgress"
      class="flex flex-row items-center gap-4 self-stretch text-slate-500 hover:text-slate-400 transition-colors bg-slate-800 rounded-b-xl p-2 pl-4 pr-3"
      :class="{ 'rounded-xl': pushNotifications.isDenied }">
      <CursorArrowRaysIcon class="min-w-6 h-6 max-md:hidden" />
      <ArrowTurnLeftDownIcon class="min-w-6 h-6 md:hidden" />
      <span class="text-base md:hidden">Valige regiooni või vajutage kandidaadi nimele, et lisada neid siin jälgimiseks
        ja kuvada rohkem infot.</span>
      <span class="text-base max-md:hidden">Vajutage kaardi regioonile või kandidaadi nimele, et lisada neid siin
        jälgimiseks ja kuvada rohkem infot.</span>
    </div>
  </div>
</template>
