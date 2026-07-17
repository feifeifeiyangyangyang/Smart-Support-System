<template>
  <section>
    <div class="page-head">
      <div>
        <h1 class="page-title">客服咨询</h1>
        <p>可咨询发货、退款、退换货、商品售后和账号问题。</p>
      </div>
      <el-button type="warning" plain :icon="Service" @click="ticketVisible = true">转人工</el-button>
    </div>

    <div class="chat-layout">
      <div class="panel conversation">
        <div class="welcome">
          您好，请直接描述问题。涉及具体订单时，可以一起提供商品名或订单号。
        </div>
        <div class="quick">
          <el-button v-for="item in quickQuestions" :key="item" size="small" round @click="ask(item)">
            {{ item }}
          </el-button>
        </div>

        <div ref="messageListRef" class="messages" aria-live="polite">
          <div v-if="!messages.length" class="empty-chat">
            <strong>可以这样问：</strong>
            <span>“暖风杯 H100 什么时候发货？”</span>
            <span>“轻氧洗面巾 C20 拆封后能退吗？”</span>
          </div>
          <div v-for="message in messages" :key="message.id" class="message" :class="message.role.toLowerCase()">
            <div class="role">{{ message.role === 'USER' ? '我' : '客服' }}</div>
            <div class="bubble">{{ message.content }}</div>
          </div>
        </div>

        <div class="input-row">
          <el-input
            v-model="question"
            aria-label="输入客服问题"
            placeholder="例如：暖风杯 H100 什么时候发货？"
            :disabled="sending"
            @keyup.enter="ask(question)"
          />
          <el-button type="primary" :icon="Promotion" :loading="sending" :disabled="!question.trim()" @click="ask(question)">
            发送
          </el-button>
          <el-button :icon="Delete" @click="clearConversation">清空</el-button>
        </div>
      </div>

      <aside class="panel side">
        <div class="side-title">参考资料</div>
        <div v-if="!lastSources.length" class="empty-source">客服回答后会显示引用到的业务资料。</div>
        <div v-for="source in lastSources" :key="source.documentId + source.fileName" class="source-item">
          <div class="source-name">{{ source.fileName }}</div>
          <p>{{ source.snippet }}</p>
        </div>
        <div class="metric">当前置信等级：{{ confidenceLabel }}</div>
        <div class="metric">是否建议人工：{{ needHuman ? '是' : '否' }}</div>

        <div class="side-title ticket-title">我的工单</div>
        <div v-if="!tickets.length" class="empty-source">暂无工单。需要人工处理时可以点击“转人工”。</div>
        <div v-for="item in tickets" :key="item.id" class="ticket-item">
          <div class="ticket-row">
            <strong>{{ item.ticketNo }}</strong>
            <el-tag size="small" :type="statusType(item.status)">{{ statusLabel(item.status) }}</el-tag>
          </div>
          <p>{{ item.description }}</p>
          <small v-if="item.handlingNote">处理备注：{{ item.handlingNote }}</small>
        </div>
      </aside>
    </div>

    <el-dialog v-model="ticketVisible" title="创建人工工单" width="460px">
      <el-form label-width="90px">
        <el-form-item label="问题分类">
          <el-select v-model="ticket.category" aria-label="问题分类">
            <el-option label="售前" value="PRE_SALE" />
            <el-option label="物流" value="DELIVERY" />
            <el-option label="退货" value="RETURN" />
            <el-option label="退款" value="REFUND" />
            <el-option label="账号" value="ACCOUNT" />
            <el-option label="售后" value="AFTER_SALE" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系方式">
          <el-input v-model="ticket.contact" aria-label="联系方式" placeholder="手机号、邮箱或微信号" />
        </el-form-item>
        <el-form-item label="问题描述">
          <el-input v-model="ticket.description" aria-label="问题描述" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ticketVisible = false">取消</el-button>
        <el-button type="primary" :loading="ticketSubmitting" @click="createTicket">提交</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete, Promotion, Service } from '@element-plus/icons-vue'
import { api, unwrap } from '../api'

interface MessageRow {
  id: number
  role: 'USER' | 'ASSISTANT' | 'SYSTEM'
  content: string
}

interface SourceReference {
  documentId: number
  fileName: string
  snippet: string
  score: number
}

interface TicketRow {
  id: number
  ticketNo: string
  category: string
  status: string
  description: string
  handlingNote?: string
}

const quickQuestions = [
  '商品什么时候发货？',
  '暖风杯 H100 什么时候发货？',
  '轻氧洗面巾 C20 拆封后能退吗？',
  '云感靠枕 P9 有污渍还能退吗？',
  '退款一般如何处理？',
  '收到破损商品怎么办？'
]
const question = ref('')
const conversationId = ref<number | null>(null)
const messages = ref<MessageRow[]>([])
const tickets = ref<TicketRow[]>([])
const lastSources = ref<SourceReference[]>([])
const confidenceLevel = ref('')
const needHuman = ref(false)
const sending = ref(false)
const ticketSubmitting = ref(false)
const ticketVisible = ref(false)
const messageListRef = ref<HTMLElement | null>(null)
const ticket = reactive({ category: 'OTHER', contact: '', description: '' })

const confidenceLabel = computed(() => {
  if (confidenceLevel.value === 'HIGH') return '高'
  if (confidenceLevel.value === 'MEDIUM') return '中'
  if (confidenceLevel.value === 'LOW') return '低'
  return '暂无'
})

onMounted(async () => {
  const conversation = await unwrap<{ id: number }>(api.post('/conversations', { title: '用户客服会话' }))
  conversationId.value = conversation.id
  await loadTickets()
})

async function reloadMessages() {
  if (!conversationId.value) return
  messages.value = await unwrap<MessageRow[]>(api.get(`/conversations/${conversationId.value}/messages`))
  await nextTick()
  messageListRef.value?.scrollTo({ top: messageListRef.value.scrollHeight, behavior: 'smooth' })
}

async function ask(text: string) {
  const content = text.trim()
  if (!content || sending.value) return
  sending.value = true
  try {
    const data = await unwrap<{
      conversationId: number
      sources: SourceReference[]
      confidenceLevel: string
      needHuman: boolean
    }>(api.post('/chat', { conversationId: conversationId.value, question: content }))
    conversationId.value = data.conversationId
    lastSources.value = data.sources
    confidenceLevel.value = data.confidenceLevel
    needHuman.value = data.needHuman
    question.value = ''
    ticket.description = content
    await reloadMessages()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发送失败')
  } finally {
    sending.value = false
  }
}

async function clearConversation() {
  if (!conversationId.value) return
  await unwrap(api.delete(`/conversations/${conversationId.value}/messages`))
  messages.value = []
  lastSources.value = []
  confidenceLevel.value = ''
  needHuman.value = false
}

async function createTicket() {
  if (!conversationId.value) return
  if (!ticket.description.trim()) {
    ElMessage.warning('请先填写问题描述')
    return
  }
  ticketSubmitting.value = true
  try {
    await unwrap(api.post('/tickets', { conversationId: conversationId.value, ...ticket }))
    ElMessage.success('工单已创建')
    ticketVisible.value = false
    await loadTickets()
  } finally {
    ticketSubmitting.value = false
  }
}

async function loadTickets() {
  const data = await unwrap<{ records: TicketRow[] }>(api.get('/tickets'))
  tickets.value = data.records
}

function statusLabel(status: string) {
  return {
    OPEN: '待处理',
    PROCESSING: '处理中',
    PENDING: '待处理',
    RESOLVED: '已解决',
    CLOSED: '已关闭'
  }[status] ?? status
}

function statusType(status: string) {
  if (status === 'RESOLVED') return 'success'
  if (status === 'PROCESSING') return 'warning'
  if (status === 'CLOSED') return 'info'
  return 'primary'
}
</script>

<style scoped>
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
}

.page-head p {
  margin: 6px 0 0;
  color: #75685f;
}

.chat-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 18px;
}

.welcome {
  margin-bottom: 14px;
  color: #6b5f58;
}

.quick {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.messages {
  min-height: 390px;
  border: 1px solid #efe8de;
  border-radius: 8px;
  padding: 14px;
  background: #fffaf5;
}

.message {
  margin-bottom: 14px;
}

.role {
  font-size: 12px;
  color: #8b7d72;
  margin-bottom: 4px;
}

.bubble {
  display: inline-block;
  max-width: min(78%, 720px);
  padding: 10px 12px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #eee2d6;
  line-height: 1.65;
  white-space: pre-wrap;
  text-align: left;
}

.message.user {
  text-align: right;
}

.message.user .bubble {
  background: #ffe9e0;
}

.input-row {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: 10px;
  margin-top: 14px;
}

.side {
  align-self: start;
}

.side-title,
.metric {
  margin-bottom: 10px;
  font-weight: 600;
}

.ticket-title {
  margin-top: 18px;
}

.empty-source {
  color: #8b7d72;
  font-size: 14px;
  line-height: 1.6;
}

.source-item,
.ticket-item {
  padding: 12px 0;
  border-bottom: 1px solid #f0e5dc;
}

.source-name {
  font-weight: 600;
  margin-bottom: 6px;
}

.source-item p,
.ticket-item p {
  margin: 0;
  color: #6b5f58;
  line-height: 1.6;
}

.ticket-row {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
  margin-bottom: 6px;
}

.ticket-item small {
  display: block;
  color: #8b7d72;
  margin-top: 6px;
}

@media (max-width: 640px) {
  .page-head {
    display: grid;
  }

  .input-row {
    grid-template-columns: 1fr 1fr;
  }

  .input-row .el-input {
    grid-column: 1 / -1;
  }

  .bubble {
    max-width: 92%;
  }
}

@media (max-width: 420px) {
  .quick .el-button {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
