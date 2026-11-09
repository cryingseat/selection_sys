<script setup>
import { computed, onMounted, ref } from 'vue'
import { publicApi } from '../../api'
import { notify } from '../../composables/useToast'
import { quotaPercent } from '../../utils/format'
import Icon from '../../components/Icon.vue'
import PageHeader from '../../components/PageHeader.vue'
import ModalDialog from '../../components/ModalDialog.vue'
import EmptyState from '../../components/EmptyState.vue'

const loading = ref(true)
const labs = ref([])
const keyword = ref('')
const activeLab = ref(null)

const filtered = computed(() => {
  const key = keyword.value.trim()
  if (!key) return labs.value
  return labs.value.filter((lab) =>
    `${lab.name}${lab.direction}${lab.intro}${(lab.mentors || []).map((m) => `${m.name}${m.researchDirection}`).join('')}`.includes(key)
  )
})

async function load() {
  loading.value = true
  try {
    labs.value = await publicApi.labs()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <PageHeader title="实训室与导师" desc="了解研究方向、培养方式、招生名额与选拔要求，再决定你的平行意向。">
    <template #actions>
      <router-link to="/student/application" class="btn btn-primary"><Icon name="form" :size="15" /> 前往填报</router-link>
    </template>
  </PageHeader>

  <div class="card card-pad" style="padding: 14px 18px">
    <div style="position: relative">
      <Icon name="search" :size="16" style="position: absolute; left: 13px; top: 11px; color: var(--muted-2)" />
      <input v-model="keyword" class="input" style="padding-left: 38px" placeholder="搜索实训室、导师或研究方向，例如「大模型」「嵌入式」「可视化」" />
    </div>
  </div>

  <div v-if="loading" class="grid-3">
    <div v-for="i in 3" :key="i" class="card card-pad"><div class="skeleton" style="height: 160px" /></div>
  </div>

  <div v-else-if="filtered.length" class="lab-grid">
    <article v-for="lab in filtered" :key="lab.id" class="card lab-card card-hover">
      <div class="lab-top">
        <div>
          <div class="lab-name">{{ lab.name }}</div>
          <div class="lab-code">{{ lab.code }}</div>
        </div>
        <span class="pill" :class="lab.remainingQuota > 0 ? 'pill--green' : 'pill--gray'">
          {{ lab.remainingQuota > 0 ? `剩余 ${lab.remainingQuota} 人` : '名额已满' }}
        </span>
      </div>
      <div class="lab-desc">{{ lab.intro }}</div>
      <div class="lab-meta">
        <span><Icon name="target" :size="13" /> {{ lab.direction }}</span>
      </div>
      <div>
        <div class="flex" style="justify-content: space-between; font-size: 11px; color: var(--muted-2)">
          <span>名额使用</span><span>{{ lab.confirmedCount || 0 }} / {{ lab.totalQuota }}</span>
        </div>
        <div class="quota-bar mt-4"><span :style="{ width: `${quotaPercent(lab.confirmedCount || 0, lab.totalQuota)}%` }" /></div>
      </div>
      <div class="mentor-list">
        <div v-for="mentor in lab.mentors || []" :key="mentor.id" class="mentor-row">
          <span><strong>{{ mentor.name }}</strong> <span class="muted tiny">{{ mentor.title }}</span></span>
          <span class="tiny muted">名额 {{ mentor.quota }}</span>
        </div>
      </div>
      <button class="btn btn-soft btn-sm btn-block" type="button" @click="activeLab = lab">查看详情 <Icon name="arrow" :size="13" /></button>
    </article>
  </div>
  <EmptyState v-else title="没有找到匹配的实训室" desc="换个关键词试试，或清空搜索。" icon="search" />

  <ModalDialog :open="Boolean(activeLab)" :title="activeLab?.name || ''" :subtitle="activeLab?.code" wide @close="activeLab = null">
    <div v-if="activeLab" class="stack">
      <div class="notice notice--info"><Icon name="target" :size="16" />{{ activeLab.direction }}</div>
      <div><div class="label">实训室简介</div><p class="small mt-4">{{ activeLab.intro }}</p></div>
      <div><div class="label">培养安排</div><p class="small mt-4">{{ activeLab.trainingPlan }}</p></div>
      <div><div class="label">选拔要求</div><p class="small mt-4">{{ activeLab.requirements }}</p></div>
      <div class="kv">
        <dt>总名额</dt><dd>{{ activeLab.totalQuota }} 人（剩余 {{ activeLab.remainingQuota }}）</dd>
        <dt>联系方式</dt><dd>{{ activeLab.contact || '—' }}</dd>
      </div>
      <div>
        <div class="label">导师名单</div>
        <div class="grid-2 mt-8">
          <div v-for="mentor in activeLab.mentors || []" :key="mentor.id" class="offer-card">
            <div class="flex" style="justify-content: space-between">
              <strong>{{ mentor.name }}</strong><span class="pill pill--gray">{{ mentor.title }}</span>
            </div>
            <div class="small muted">{{ mentor.researchDirection }}</div>
            <p class="tiny muted">{{ mentor.profile }}</p>
            <div class="small">招生名额 <strong>{{ mentor.quota }}</strong> 人</div>
          </div>
        </div>
      </div>
    </div>
    <template #footer>
      <button class="btn btn-ghost" type="button" @click="activeLab = null">关闭</button>
      <router-link to="/student/application" class="btn btn-primary">去填报这个方向</router-link>
    </template>
  </ModalDialog>
</template>
