import { ref, computed } from 'vue';
import { defineStore } from 'pinia';
import axios from 'axios';

const VAPID_PUBLIC_KEY = import.meta.env.VAPID_PUBLIC_KEY || 'YOUR_VAPID_PUBLIC_KEY_HERE';

export const usePushNotifications = defineStore('pushNotifications', () => {
  const isSupported = ref(false);
  const isSubscribed = ref(false);
  const subscription = ref<PushSubscription | null>(null);
  const permission = ref<NotificationPermission>('default');
  const error = ref<string | null>(null);

  const checkSupport = () => {
    isSupported.value = 'serviceWorker' in navigator &&
                        'PushManager' in window &&
                        'Notification' in window;

    if (isSupported.value && Notification.permission) {
      permission.value = Notification.permission;
    }

    return isSupported.value;
  };

  const urlBase64ToUint8Array = (base64String: string): Uint8Array<ArrayBuffer> => {
    const padding = '='.repeat((4 - base64String.length % 4) % 4);
    const base64 = (base64String + padding)
      .replace(/\-/g, '+')
      .replace(/_/g, '/');

    const rawData = window.atob(base64);
    const outputArray = new Uint8Array(rawData.length);

    for (let i = 0; i < rawData.length; ++i) {
      outputArray[i] = rawData.charCodeAt(i);
    }
    return outputArray;
  };

  const registerServiceWorker = async (): Promise<ServiceWorkerRegistration | null> => {
    try {
      const registration = await navigator.serviceWorker.register('/sw.js', {
        scope: '/'
      });
      console.log('Service Worker registered:', registration);
      return registration;
    } catch (err) {
      console.error('Service Worker registration failed:', err);
      error.value = 'Failed to register service worker';
      return null;
    }
  };

  const requestPermission = async (): Promise<boolean> => {
    if (!isSupported.value) {
      error.value = 'Push notifications are not supported';
      return false;
    }

    try {
      const result = await Notification.requestPermission();
      permission.value = result;

      if (result === 'granted') {
        console.log('Notification permission granted');
        return true;
      } else {
        error.value = 'Notification permission denied';
        return false;
      }
    } catch (err) {
      console.error('Error requesting permission:', err);
      error.value = 'Failed to request permission';
      return false;
    }
  };

  const subscribe = async (): Promise<PushSubscription | null> => {
    try {
      error.value = null;

      if (!checkSupport()) {
        throw new Error('Push notifications not supported');
      }

      const hasPermission = await requestPermission();
      if (!hasPermission) {
        throw new Error('Permission not granted');
      }

      const registration = await registerServiceWorker();
      if (!registration) {
        throw new Error('Service worker registration failed');
      }

      await navigator.serviceWorker.ready;

      const sub = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(VAPID_PUBLIC_KEY)
      });

      subscription.value = sub;
      isSubscribed.value = true;

      await sendSubscriptionToBackend(sub);

      console.log('Push subscription:', sub);
      return sub;
    } catch (err) {
      console.error('Error subscribing:', err);
      error.value = err instanceof Error ? err.message : 'Subscription failed';
      return null;
    }
  };

  const unsubscribe = async (): Promise<boolean> => {
    try {
      if (!subscription.value) {
        const registration = await navigator.serviceWorker.ready;
        subscription.value = await registration.pushManager.getSubscription();
      }

      if (subscription.value) {
        await subscription.value.unsubscribe();
        await removeSubscriptionFromBackend(subscription.value);
        subscription.value = null;
        isSubscribed.value = false;
        return true;
      }
      return false;
    } catch (err) {
      console.error('Error unsubscribing:', err);
      error.value = 'Failed to unsubscribe';
      return false;
    }
  };

  const sendSubscriptionToBackend = async (sub: PushSubscription): Promise<void> => {
    try {
      await axios.post('/api/push/subscribe', sub.toJSON());
      console.log('Subscription saved to server');
    } catch (err) {
      console.error('Error sending subscription to backend:', err);
      throw err;
    }
  };

  const removeSubscriptionFromBackend = async (sub: PushSubscription): Promise<void> => {
    try {
      await axios.post('/api/push/unsubscribe', sub.toJSON());
      console.log('Subscription removed from server');
    } catch (err) {
      console.error('Error removing subscription from backend:', err);
    }
  };

  const checkExistingSubscription = async (): Promise<void> => {
    try {
      if (!checkSupport()) return;

      const registration = await navigator.serviceWorker.ready;
      const sub = await registration.pushManager.getSubscription();

      if (sub) {
        subscription.value = sub;
        isSubscribed.value = true;
        console.log('Existing subscription found:', sub);
      }
    } catch (err) {
      console.error('Error checking subscription:', err);
    }
  };

  const shouldPrompt = computed(() => {
    return isSupported.value && (permission.value === 'default' || permission.value === 'granted') && !isSubscribed.value
  });

  const isDenied = computed(() => {
    return permission.value === 'denied';
  });

  // Initialize
  checkSupport();
  if (isSupported.value) {
    checkExistingSubscription();
  }

  return {
    subscription,
    permission,
    error,
    isSupported,
    isSubscribed,
    isDenied,
    shouldPrompt,
    subscribe,
    unsubscribe,
    requestPermission,
  };
});

