<template>
  <section>
    <h1 class="page-title">管理后台</h1>
    <el-tabs>
      <el-tab-pane label="文档管理">
        <div class="panel">
          <div class="toolbar">
            <el-upload :http-request="uploadDocument" :show-file-list="false">
              <el-button type="primary" :icon="UploadFilled">上传文档</el-button>
            </el-upload>
            <el-input v-model="keyword" class="toolbar-input" placeholder="按名称搜索" clearable @keyup.enter="loadDocuments" />
            <el-button :icon="Search" @click="loadDocuments">搜索</el-button>
          </div>
          <el-table :data="documents" border stripe v-loading="documentLoading">
            <el-table-column prop="originalName" label="文档名" min-width="220" />
            <el-table-column prop="fileType" label="类型" width="90" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" effect="light">{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="chunkCount" label="片段数" width="100" />
            <el-table-column prop="failureReason" label="失败原因" min-width="180" />
            <el-table-column label="操作" width="220">
              <template #default="{ row }">
                <el-button text type="primary" @click="download(row.id)">下载</el-button>
                <el-button v-if="row.status === 'FAILED'" text type="warning" @click="retry(row.id)">重试</el-button>
                <el-button text type="danger" @click="remove(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
      <el-tab-pane label="工单处理">
        <div class="panel">
          <div class="toolbar">
            <el-select v-model="ticketStatus" clearable placeholder="状态筛选" class="toolbar-select" @change="loadTickets">
              <el-option label="待处理" value="PENDING" />
              <el-option label="处理中" value="PROCESSING" />
              <el-option label="已解决" value="RESOLVED" />
              <el-option label="已关闭" value="CLOSED" />
            </el-select>
            <el-button :icon="Refresh" @click="loadTickets">刷新</el-button>
          </div>
          <el-table :data="tickets" border stripe v-loading="ticketLoading">
            <el-table-column prop="ticketNo" label="工单编号" min-width="180" />
            <el-table-column prop="category" label="分类" width="120" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" effect="light">{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述" min-width="220" />
            <el-table-column label="处理" width="260">
              <template #default="{ row }">
                <el-select v-model="row.nextStatus" placeholder="新状态" style="width: 120px">
                  <el-option label="处理中" value="PROCESSING" />
                  <el-option label="已解决" value="RESOLVED" />
                  <el-option label="已关闭" value="CLOSED" />
                </el-select>
                <el-button text type="primary" @click="updateTicket(row)">保存</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, type UploadRequestOptions } from 'element-plus'
import { Refresh, Search, UploadFilled } from '@element-plus/icons-vue'
import { api, unwrap } from '../api'

interface DocumentRow {
  id: number
  originalName: string
  fileType: string
  status: string
  chunkCount: number
  failureReason?: string
}

interface TicketRow {
  id: number
  ticketNo: string
  category: string
  status: string
  description: string
  nextStatus?: string
}

const keyword = ref('')
const documents = ref<DocumentRow[]>([])
const tickets = ref<TicketRow[]>([])
const ticketStatus = ref('')
const documentLoading = ref(false)
const ticketLoading = ref(false)

onMounted(async () => {
  await Promise.all([loadDocuments(), loadTickets()])
})

async function loadDocuments() {
  documentLoading.value = true
  try {
    const data = await unwrap<{ records: DocumentRow[] }>(api.get('/admin/documents', { params: { keyword: keyword.value } }))
    documents.value = data.records
  } finally {
    documentLoading.value = false
  }
}

async function uploadDocument(options: UploadRequestOptions) {
  const form = new FormData()
  form.append('file', options.file)
  try {
    await unwrap(api.post('/admin/documents', form, { headers: { 'Content-Type': 'multipart/form-data' } }))
    ElMessage.success('上传完成')
    await loadDocuments()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '上传失败')
  }
}

function download(id: number) {
  window.open(`/api/v1/admin/documents/${id}/download`, '_blank')
}

async function retry(id: number) {
  await unwrap(api.post(`/admin/documents/${id}/retry`))
  await loadDocuments()
}

async function remove(id: number) {
  await unwrap(api.delete(`/admin/documents/${id}`))
  await loadDocuments()
}

async function loadTickets() {
  const params: Record<string, string> = {}
  if (ticketStatus.value) params.status = ticketStatus.value
  ticketLoading.value = true
  try {
    const data = await unwrap<{ records: TicketRow[] }>(api.get('/admin/tickets', { params }))
    tickets.value = data.records
  } finally {
    ticketLoading.value = false
  }
}

async function updateTicket(row: TicketRow) {
  if (!row.nextStatus) return
  await unwrap(api.patch(`/admin/tickets/${row.id}/status`, { status: row.nextStatus, handlingNote: '后台已处理' }))
  await loadTickets()
}

function statusLabel(status: string) {
  return {
    COMPLETED: '已完成',
    PROCESSING: '处理中',
    PENDING: '待处理',
    FAILED: '失败',
    RESOLVED: '已解决',
    CLOSED: '已关闭'
  }[status] ?? status
}

function statusType(status: string) {
  if (status === 'COMPLETED' || status === 'RESOLVED') return 'success'
  if (status === 'PROCESSING') return 'warning'
  if (status === 'FAILED') return 'danger'
  if (status === 'CLOSED') return 'info'
  return 'primary'
}
</script>
