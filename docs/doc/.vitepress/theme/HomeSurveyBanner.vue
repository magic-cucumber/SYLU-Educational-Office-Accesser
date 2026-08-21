<script setup lang="ts">
import { inBrowser, useRoute } from 'vitepress'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const route = useRoute()
const isHome = computed(() => route.path === '/' || route.path === '/index.html')
const banner = ref<HTMLElement | null>(null)
let resizeObserver: ResizeObserver | undefined

function syncLayoutTopHeight() {
  if (!inBrowser) return

  const height = isHome.value && banner.value ? `${banner.value.offsetHeight}px` : '0px'
  document.documentElement.style.setProperty('--vp-layout-top-height', height)
}

function observeBanner() {
  resizeObserver?.disconnect()
  resizeObserver = undefined

  if (inBrowser && banner.value) {
    resizeObserver = new ResizeObserver(syncLayoutTopHeight)
    resizeObserver.observe(banner.value)
  }
}

onMounted(() => {
  syncLayoutTopHeight()
  observeBanner()
})

watch(isHome, async () => {
  await nextTick()
  syncLayoutTopHeight()
  observeBanner()
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  if (inBrowser) {
    document.documentElement.style.removeProperty('--vp-layout-top-height')
  }
})
</script>

<template>
  <div v-if="isHome" ref="banner" class="survey-banner" role="banner">
    <div class="survey-banner__content">
      <span class="survey-banner__emoji" aria-hidden="true">🎉</span>
      <span>我们需要听到你的声音——EOA问卷调查</span>
      <span class="survey-banner__arrow" aria-hidden="true">→</span>
      <a
        class="survey-banner__link"
        href="https://wj.qq.com/s2/27558699/2htx/"
        target="_blank"
        rel="noopener noreferrer"
      >点击即可填写</a>
    </div>
  </div>
</template>
