<script setup>
import { onMounted, ref } from 'vue'
import { adminApi } from '../../api'
import { notify } from '../../composables/useToast'
import { fmt } from '../../utils/format'
import Icon from '../../components/Icon.vue'
import PageHeader from '../../components/PageHeader.vue'
import EmptyState from '../../components/EmptyState.vue'

const loading = ref(true)
const records = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const batchId = ref('')
const action = ref('')
const batches = ref([])

async function load() {
  loading.value = true
  try {
    if (!batches.value.length) batches.value = await adminApi.batches()
    const data = await adminApi.auditLogs({
      batchId: batchId.value || undefined,
      action: action.value || undefined,
      page: page.value,
      size: size.value
    })
    records.value = data.records
    total.value = data.total
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    loading.value = false
  }
}

onMounted(load)

function search() {
  page.value = 1
  load()
}
</script>

<template>
  <PageHeader title="操作审计" desc="记录报名、面试、拟录取、确认、补录、调剂、公示与阶段切换等关键操作。">
    <template #actions>
      <button class="btn btn-ghost" type="button" @click="search"><Icon name="refresh" :size="15" /> 刷新</button>
    </template>
  </PageHeader>

  <div class="card card-pad" style="padding: 13px 18px">
    <div class="flex flex-wrap" style="justify-content: space-between; gap: 10px">
      <div class="flex" style="gap: 8px">
        <select v-model="batchId" class="select" style="width: 240px" @change="search">
          <option value="">全部批次</option>
          <option v-for="batch in batches" :key="batch.id" :value="batch.id">{{ batch.name }}</option>
        </select>
        <input v-model="action" class="input" style="width: 220px" placeholder="操作类型，如 OFFER_CREATE" @keyup.enter="search" />
        <button class="btn btn-soft btn-sm" @click="search">查询</button>
      </div>
      <span class="small muted">共 {{ total }} 条</span>
    </div>
  </div>

  <div class="card">
    <div v-if="loading" class="loading-block"><div class="spinner" />正在加载审计日志…</div>
    <div v-else-if="records.length" class="table-wrap" style="border: 0">
      <table class="table">
        <thead><tr><th>时间</th><th>操作人</th><th>操作</th><th>对象</th><th>详情</th><th>原因</th><th>IP</th></tr></thead>
        <tbody>
          <tr v-for="log in records" :key="log.id">
            <td class="nowrap">{{ fmt(log.createdAt) }}</td>
            <td><strong>{{ log.actorName || '—' }}</strong><div class="tiny muted">{{ log.actorRole }}</div></td>
            <td><span class="pill pill--blue">{{ log.action }}</span></td>
            <td><span class="tiny muted">{{ log.targetType }} #{{ log.targetId }}</span></td>
            <td><span class="tiny">{{ log.detail }}</span></td>
            <td><span class="tiny muted">{{ log.reason || '—' }}</span></td>
            <td><span class="tiny muted">{{ log.ip || '—' }}</span></td>
          </tr>
        </tbody>
      </table>
    </div>
    <EmptyState v-else title="暂无审计记录" desc="关键业务操作会自动写入日志。" icon="chart" />
    <div v-if="total > size" class="flex" style="justify-content: space-between; padding: 12px 16px; border-top: 1px solid var(--line)">
      <span class="small muted">第 {{ page }} 页</span>
      <div class="button-row">
        <button class="btn btn-ghost btn-sm" :disabled="page <= 1" @click="page--; load()">上一页</button>
        <button class="btn btn-ghost btn-sm" :disabled="page * size >= total" @click="page++; load()">下一页</button>
      </div>
    </div>
  </div>
</template>
