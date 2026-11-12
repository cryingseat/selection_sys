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
const onlyAvailable = ref(false)
const activeLab = ref(null)

const filtered = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  return labs.value.filter((lab) => {
    if (onlyAvailable.value && !(lab.remainingQuota > 0)) return false
    if (!key) return true
    return `${lab.name}${lab.code}${lab.direction}${lab.intro}${(lab.mentors || []).map((m) => `${m.name}${m.researchDirection}`).join('')}`.toLowerCase().includes(key)
  })
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

function directionTags(lab) {
  return (lab.direction || '').split(/[、,，]/).filter(Boolean).slice(0, 4)
}
</script>

<template>
  <PageHeader title="实训室与导师" desc="仅展示你所在学院的实训空间与导师方向；游客可浏览全校实训室。">
    <template #actions>
      <router-link to="/student/application" class="btn btn-primary"><Icon name="form" :size="15" /> 前往填报</router-link>
    </template>
  </PageHeader>

  <div class="gallery-toolbar" style="margin-top: 0">
    <div class="filter-pills">
      <span class="pill pill--gray">{{ labs.length }} 个本学院实训室</span>
    </div>
    <div class="gallery-tools">
      <div class="search-pill">
        <Icon name="search" :size="14" />
        <input v-model="keyword" placeholder="搜索实训室、导师或研究方向" />
        <button v-if="keyword" class="clear" type="button" @click="keyword = ''">×</button>
      </div>
      <button class="filter-pill" :class="{ active: onlyAvailable }" @click="onlyAvailable = !onlyAvailable">仅看有余量</button>
    </div>
  </div>

  <div v-if="loading" class="studio-grid">
    <div v-for="i in 2" :key="i" class="studio-card"><div class="skeleton" style="height: 380px" /></div>
  </div>

  <div v-else-if="filtered.length" class="studio-grid">
    <article v-for="lab in filtered" :key="lab.id" class="studio-card">
      <div class="studio-cover">
        <img v-if="lab.coverImage" :src="lab.coverImage" :alt="lab.name" />
        <div v-else class="cover-fallback">{{ lab.code }}</div>
        <div class="studio-badges">
          <div class="studio-badges-left">
            <span class="badge-light">{{ lab.collegeName }}</span>
            <span class="badge-dark">{{ lab.location || lab.contact }}</span>
          </div>
          <span class="badge-light">{{ lab.remainingQuota > 0 ? `余 ${lab.remainingQuota}` : '名额已满' }}</span>
        </div>
        <div class="studio-title">
          <div>
            <p>{{ lab.code }}</p>
            <h2>{{ lab.name }}</h2>
          </div>
          <span class="studio-quota">共招 {{ lab.totalQuota }} 人</span>
        </div>
      </div>
      <div class="studio-vibe">
        <p>{{ lab.intro }}</p>
        <div class="vibe-tags">
          <span v-for="tag in directionTags(lab)" :key="tag">{{ tag }}</span>
        </div>
      </div>
      <div class="studio-mentors">
        <div class="studio-mentors-head">
          <span>驻室导师（{{ (lab.mentors || []).length }}）</span>
          <span>名额使用 {{ lab.confirmedCount || 0 }} / {{ lab.totalQuota }}</span>
        </div>
        <div class="mentor-mini-grid">
          <div v-for="mentor in lab.mentors || []" :key="mentor.id" class="mentor-mini" style="cursor: default">
            <span class="mentor-avatar" :style="{ backgroundColor: lab.collegeAccent || '#294a72' }">{{ mentor.name.slice(0, 1) }}</span>
            <span class="mentor-mini-body">
              <strong>{{ mentor.name }} <span>{{ mentor.title }}</span></strong>
              <em>招录 {{ mentor.quota }} 人 · {{ mentor.applicantCount || 0 }} 报</em>
              <small>{{ mentor.researchDirection }}</small>
            </span>
          </div>
        </div>
        <div class="quota-bar"><span :style="{ width: `${quotaPercent(lab.confirmedCount || 0, lab.totalQuota)}%`, background: 'var(--ink)' }" /></div>
      </div>
      <div class="studio-actions">
        <button class="btn btn-ghost btn-sm" type="button" @click="activeLab = lab">查看实训室详情</button>
        <router-link to="/student/application" class="btn btn-primary btn-sm">选择导师</router-link>
      </div>
    </article>
  </div>
  <EmptyState v-else title="没有找到匹配的实训室" desc="换个关键词试试，或清空搜索。" icon="search" />

  <ModalDialog :open="Boolean(activeLab)" :title="activeLab?.name || ''" :subtitle="activeLab?.code" wide @close="activeLab = null">
    <div v-if="activeLab" class="stack">
      <div class="notice notice--info"><Icon name="target" :size="16" />{{ activeLab.direction }}</div>
      <div class="grid-2">
        <div><div class="label">所属学院</div><p class="small mt-4">{{ activeLab.collegeName }}</p></div>
        <div><div class="label">空间位置</div><p class="small mt-4">{{ activeLab.location || activeLab.contact }}</p></div>
      </div>
      <div><div class="label">实训室简介</div><p class="small mt-4">{{ activeLab.intro }}</p></div>
      <div><div class="label">培养安排</div><p class="small mt-4">{{ activeLab.trainingPlan }}</p></div>
      <div><div class="label">选拔要求</div><p class="small mt-4">{{ activeLab.requirements }}</p></div>
      <div>
        <div class="label">导师名单</div>
        <div class="grid-2 mt-8">
          <div v-for="mentor in activeLab.mentors || []" :key="mentor.id" class="offer-card">
            <div class="flex" style="justify-content: space-between">
              <strong>{{ mentor.name }}</strong><span class="pill pill--gray">{{ mentor.title }}</span>
            </div>
            <div class="small muted">{{ mentor.researchDirection }}</div>
            <p class="tiny muted">{{ mentor.profile }}</p>
            <div class="small">招生名额 <strong>{{ mentor.quota }}</strong> 人 · 已用 {{ mentor.confirmedCount || 0 }} 人</div>
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
