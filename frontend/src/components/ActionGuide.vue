<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import Icon from './Icon.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  stage: { type: String, default: '' },
  title: { type: String, default: '' },
  desc: { type: String, default: '' },
  actionLabel: { type: String, default: '' },
  target: { type: String, default: '' },
  countdown: { type: Number, default: 5 }
})
const emit = defineEmits(['close', 'go'])

const remain = ref(props.countdown)
let timer = null

watch(
  () => props.open,
  (open) => {
    if (open) {
      remain.value = props.countdown
      start()
    } else {
      stop()
    }
  }
)

function start() {
  stop()
  if (!props.target) return
  timer = setInterval(() => {
    remain.value -= 1
    if (remain.value <= 0) {
      stop()
      emit('go')
    }
  }, 1000)
}
function stop() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}
function onGo() {
  stop()
  emit('go')
}
function onClose() {
  stop()
  emit('close')
}
onBeforeUnmount(stop)
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="open" class="modal-backdrop" @click.self="onClose">
        <div class="modal guide-modal" role="dialog" aria-modal="true">
          <div class="guide-hero">
            <div class="guide-kicker">接下来做什么</div>
            <h3>{{ title }}</h3>
            <p v-if="desc">{{ desc }}</p>
          </div>

          <div v-if="target" class="guide-action">
            <div class="guide-action-head">
              <span class="pill pill--orange">{{ stage || '待办' }}</span>
              <span v-if="remain > 0" class="guide-countdown">
                <span class="guide-ring">{{ remain }}</span> 秒后自动前往
              </span>
            </div>
            <button class="btn btn-primary btn-lg btn-block" type="button" @click="onGo">
              {{ actionLabel || '立即前往' }} <Icon name="arrow" :size="16" />
            </button>
          </div>

          <div class="guide-foot">
            <button class="btn btn-ghost btn-sm" type="button" @click="onClose">稍后再说</button>
            <span class="guide-hint">之后可随时从左侧菜单进入对应功能</span>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.18s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>

<style>
.guide-modal {
  width: min(460px, 100%);
  padding: 0;
  overflow: hidden;
  border: 1px solid var(--line);
  box-shadow: var(--shadow-lg);
}
.guide-hero {
  padding: 26px 26px 22px;
  background:
    radial-gradient(120% 120% at 100% 0, rgba(185, 212, 88, 0.18), transparent 55%),
    linear-gradient(135deg, #f8fbf0, #fbe9ee);
}
.guide-kicker {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: var(--gold-deep, #6d9612);
  margin-bottom: 10px;
}
.guide-kicker::before {
  content: "";
  width: 20px;
  height: 2px;
  border-radius: 2px;
  background: linear-gradient(90deg, var(--gold, #a3c23b), transparent);
}
.guide-hero h3 {
  font-family: var(--serif);
  font-size: 22px;
  line-height: 1.35;
  font-weight: 700;
  color: var(--ink);
}
.guide-hero p {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--muted);
}
.guide-action {
  padding: 20px 26px;
}
.guide-action-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.guide-countdown {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  color: var(--muted);
}
.guide-ring {
  display: inline-grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid var(--primary, #4a6714);
  color: var(--primary, #4a6714);
  font-weight: 800;
  font-size: 12px;
}
.guide-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 26px;
  border-top: 1px solid var(--line);
  background: var(--surface-2, #f4f7ec);
}
.guide-hint {
  font-size: 11px;
  color: var(--muted-2);
}
</style>
