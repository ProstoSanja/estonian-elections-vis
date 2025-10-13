import './main.css'

import {createApp} from 'vue'
import {createPinia} from 'pinia'
import {VueQueryPlugin} from '@tanstack/vue-query'

import { use } from "echarts/core";
import { CanvasRenderer } from "echarts/renderers";
import { MapChart } from "echarts/charts";
import { TooltipComponent, VisualMapComponent } from "echarts/components";

import App from './App.vue'
import router from './router.ts'

use([CanvasRenderer, MapChart, TooltipComponent, VisualMapComponent]);

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(VueQueryPlugin, {
  enableDevtoolsV6Plugin: true,
})

app.mount('#app')
