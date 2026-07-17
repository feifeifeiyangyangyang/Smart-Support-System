<template>
  <section>
    <div class="page-head">
      <div>
        <h1 class="page-title">客服咨询</h1>
        <p>可咨询发货、物流、退款、退换货、商品售后和账号问题。涉及订单时，系统会优先查询订单数据。</p>
      </div>
      <el-button type="warning" plain :icon="Service" @click="ticketVisible = true">转人工</el-button>
    </div>

    <div class="chat-layout">
      <div class="panel conversation">
        <div class="welcome">您好，请直接描述问题。可以带上订单号或商品名，我会先帮您查订单和物流。</div>
        <div class="quick">
          <el-button v-for="item in quickQuestions" :key="item" size="small" round @click="ask(item)">
            {{ item }}
          </el-button>
        </div>

        <div ref="messageListRef" class="messages" aria-live="polite">
          <div v-if="!messages.length" class="empty-chat">
            <strong>可以这样问：</strong>
            <span>“我的订单什么时候发货？”</span>
            <span>“ORD202607160002 物流到哪了？”</span>
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
            placeholder="例如：我的订单什么时候发货？"
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
        <div class="side-title">我的订单</div>
        <div v-if="!orders.length" class="empty-source">暂无订单。</div>
        <div v-for="order in orders" :key="order.id" class="order-item">
          <div class="ticket-row">
            <strong>{{ order.orderNo }}</strong>
            <el-tag size="small" :type="orderStatusType(order.status)">{{ orderStatusLabel(order.status) }}</el-tag>
          </div>
          <p>{{ order.product.productName }} × {{ order.quantity }}</p>
          <small>预计发货：{{ formatDate(order.expectedShipAt) }}</small>
          <small v-if="order.shipmentEvents.length">最新物流：{{ order.shipmentEvents[0].eventNote }}</small>
        </div>

        <div class="side-title source-title">引用资料</div>
        <div v-if="!lastSources.length" class="empty-source">客服回答引用知识库时会显示来源。</div>
        <div v-for="source in lastSources" :key="source.documentId + source.fileName" class="source-item">
          <div class="source-name">{{ source.fileName }}</div>
          <p>{{ source.snippet }}</p>
        </div>
        <div class="metric">置信等级：{{ confidenceLabel }}</div>
        <div class="metric">建议人工：{{ needHuman ? '是' : '否' }}</div>

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

interface ProductRow {
  id: number
  productCode: string
  productName: string
}

interface ShipmentEvent {
  eventNote: string
  eventTime: string
  trackingNo?: string
}

interface OrderRow {
  id: number
  orderNo: string
  product: ProductRow
  quantity: number
  status: string
  expectedShipAt?: string
  shipmentEvents: ShipmentEvent[]
}

const quickQuestions = [
  '我的订单什么时候发货？',
  'ORD202607160002 物流到哪了？',
  '暖风杯 H100 还有库存吗？',
  '轻氧洗面巾 C20 拆封后能退吗？',
  '收到破损商品怎么办？'
]
const question = ref('')
const conversationId = ref<number | null>(null)
const messages = ref<MessageRow[]>([])
const tickets = ref<TicketRow[]>([])
const orders = ref<OrderRow[]>([])
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
  await Promise.all([loadTickets(), loadOrders()])
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
    await loadOrders()
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

async function loadOrders() {
  const data = await unwrap<{ records: OrderRow[] }>(api.get('/orders'))
  orders.value = data.records
}

function statusLabel(status: string) {
  return {
    OPEN: '待处理',
    PROCESSING: '处理中',
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

function orderStatusLabel(status: string) {
  return {
    PENDING_PAYMENT: '待付款',
    PAID: '已付款',
    WAITING_SHIPMENT: '待发货',
    SHIPPED: '已发货',
    IN_TRANSIT: '运输中',
    SIGNED: '已签收',
    REFUNDING: '退款中',
    REFUNDED: '已退款',
    CANCELLED: '已取消'
  }[status] ?? status
}

function orderStatusType(status: string) {
  if (status === 'SIGNED') return 'success'
  if (status === 'IN_TRANSIT' || status === 'SHIPPED') return 'warning'
  if (status === 'CANCELLED' || status === 'REFUNDED') return 'info'
  return 'primary'
}

function formatDate(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '暂未同步'
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
  grid-template-columns: minmax(0, 1fr) 340px;
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

.empty-chat {
  display: grid;
  gap: 8px;
  color: #75685f;
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

.source-title,
.ticket-title {
  margin-top: 18px;
}

.empty-source {
  color: #8b7d72;
  font-size: 14px;
  line-height: 1.6;
}

.source-item,
.ticket-item,
.order-item {
  padding: 12px 0;
  border-bottom: 1px solid #f0e5dc;
}

.source-name {
  font-weight: 600;
  margin-bottom: 6px;
}

.source-item p,
.ticket-item p,
.order-item p {
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

.ticket-item small,
.order-item small {
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
