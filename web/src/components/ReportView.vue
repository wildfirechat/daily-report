<template>
    <main class="page detail-page">
        <header class="detail-nav">
            <button class="text-button" type="button" @click="backToList">‹ {{ backLabel }}</button>
            <div v-if="report && isOwner" class="detail-actions">
                <button type="button" @click="editReport">编辑</button>
                <button class="danger-button" type="button" @click="requestDelete">删除</button>
            </div>
        </header>

        <p v-if="actionError" class="error-banner" role="alert">{{ actionError }}</p>

        <section v-if="loading" class="state-view" aria-busy="true">正在加载日报...</section>
        <section v-else-if="errorMessage" class="state-view error-state" role="alert">
            <strong>{{ errorMessage }}</strong>
            <p v-if="!terminalError">请检查网络后重试，或返回日报列表。</p>
            <div class="error-actions">
                <button v-if="!terminalError" class="primary-button" type="button" @click="getReport">重新加载</button>
                <button class="secondary-button" type="button" @click="backToList">{{ backLabel }}</button>
            </div>
        </section>
        <article v-else-if="report" class="detail-card">
            <header class="detail-header">
                <p class="eyebrow">工作日报</p>
                <h1>{{ report.title || '日报' }}</h1>
                <p class="report-date">{{ formatDate(report.day) }}</p>
                <div class="author-line"><span class="avatar">{{ authorInitial }}</span><strong>{{ report.displayName || report.userId }}</strong></div>
            </header>
            <section class="detail-section">
                <h2>今日工作总结</h2>
                <p>{{ report.content }}</p>
            </section>
            <section class="detail-section">
                <h2>明日工作计划</h2>
                <p :class="{placeholder: !report.tomorrowPlan}">{{ report.tomorrowPlan || '未填写' }}</p>
            </section>
            <section class="detail-section">
                <h2>需要帮助与协调</h2>
                <p :class="{placeholder: !report.requirement}">{{ report.requirement || '暂无需要协调的事项' }}</p>
            </section>
            <footer class="detail-meta">
                <p v-if="report.createdAt">首次提交：{{ formatDateTime(report.createdAt) }}</p>
                <p v-if="report.updatedAt">最近更新：{{ formatDateTime(report.updatedAt) }}</p>
                <p>汇报对象 {{ (report.reportTo || []).length }} 人<span v-if="report.ccTo && report.ccTo.length"> · 抄送 {{ report.ccTo.length }} 人</span></p>
            </footer>
        </article>

        <ConfirmDialog :visible="showDeleteConfirm"
                       title="删除日报"
                       :message="deleteMessage"
                       confirm-text="删除"
                       loading-text="删除中..."
                       danger
                       :loading="deleting"
                       @confirm="confirmDelete"
                       @cancel="showDeleteConfirm = false"/>
    </main>
</template>

<script>
import api from "@/api/api";
import ConfirmDialog from "@/components/ConfirmDialog";
import {formatDate, formatDateTime} from '@/utils/date'
import {queueToast} from '@/utils/toast'
import {openPage} from '@/utils/navigation'

export default {
    name: "ReportView",
    components: {ConfirmDialog},
    props: {account: {type: String, required: true}},
    data() {
        return {
            report: null,
            loading: true,
            errorMessage: '',
            terminalError: false,
            actionError: '',
            showDeleteConfirm: false,
            deleting: false
        }
    },
    computed: {
        isOwner() {
            return this.report && this.report.userId === this.account
        },
        authorInitial() {
            const name = this.report.displayName || this.report.userId || '日'
            return name.substring(0, 1).toUpperCase()
        },
        deleteMessage() {
            if (!this.report) {
                return '确定删除这篇日报吗？删除后无法恢复。'
            }
            return `确定删除《${this.report.title || '日报'}》吗？删除后无法恢复。`
        },
        fromMode() {
            return new URLSearchParams(window.location.search).get('from')
        },
        backLabel() {
            if (this.fromMode === 'received' || (this.fromMode !== 'mine' && this.report && !this.isOwner)) {
                return '返回汇报箱'
            }
            return '返回我的日报'
        }
    },
    created() {
        this.getReport()
    },
    methods: {
        formatDate,
        formatDateTime,
        async getReport() {
            this.loading = true
            this.errorMessage = ''
            this.actionError = ''
            this.terminalError = false
            const params = new URLSearchParams(window.location.search)
            const userId = params.get('userId') || this.account
            const day = Number(params.get('day')) || Date.now()
            try {
                this.report = await api.getReport(userId, day)
            } catch (e) {
                this.report = null
                this.terminalError = e.code === 16
                this.errorMessage = e.code === 16
                    ? '日报不存在或无权查看'
                    : (e.message || '日报加载失败')
            } finally {
                this.loading = false
            }
        },
        editReport() {
            openPage(`newReport.html?day=${this.report.day}`)
        },
        requestDelete() {
            this.actionError = ''
            this.showDeleteConfirm = true
        },
        async confirmDelete() {
            if (this.deleting) return
            this.deleting = true
            try {
                await api.deleteReport(this.report.day)
                queueToast('日报已删除')
                openPage('myReports.html')
            } catch (e) {
                this.showDeleteConfirm = false
                this.actionError = e.message || '删除失败，请稍后重试'
            } finally {
                this.deleting = false
            }
        },
        backToList() {
            openPage(this.backLabel === '返回汇报箱' ? 'inbox.html' : 'myReports.html')
        }
    }
}
</script>

<style scoped>
.detail-page { padding-top: 16px; }
.detail-nav { display: flex; align-items: center; justify-content: space-between; min-height: 48px; margin-bottom: 12px; }
.text-button, .detail-actions button { min-height: 44px; padding: 8px 10px; border: 0; background: transparent; color: #667085; font-size: 14px; cursor: pointer; }
.text-button { padding-left: 0; }
.detail-actions { display: flex; gap: 4px; }
.detail-actions button { color: #1d4ed8; }
.detail-actions .danger-button { color: #d92d20; }
.text-button:focus-visible, .detail-actions button:focus-visible { outline: 3px solid rgba(47, 111, 237, .28); outline-offset: 2px; border-radius: 8px; }
.detail-card { overflow: hidden; border: 1px solid #e4e7ec; border-radius: 14px; background: #fff; text-align: left; }
.detail-header { padding: 26px 22px 20px; background: linear-gradient(145deg, #f2f6ff, #fff); }
.eyebrow { color: #2f6fed; font-size: 11px; font-weight: 600; letter-spacing: 3px; }
.detail-header h1 { margin-top: 8px; color: #1f2937; font-size: 25px; line-height: 1.4; }
.report-date { margin-top: 8px; color: #667085; font-size: 13px; }
.author-line { display: flex; align-items: center; gap: 9px; margin-top: 20px; color: #1f2937; font-size: 14px; }
.avatar { display: inline-flex; align-items: center; justify-content: center; width: 32px; height: 32px; border-radius: 50%; background: #2f6fed; color: #fff; }
.detail-section { padding: 22px; border-top: 1px solid #edf0f4; }
.detail-section h2 { color: #475467; font-size: 14px; }
.detail-section p { margin-top: 12px; color: #344054; font-size: 15px; line-height: 1.85; overflow-wrap: anywhere; white-space: pre-wrap; }
.detail-section p.placeholder { color: #98a2b3; }
.detail-meta { padding: 16px 22px; border-top: 1px solid #edf0f4; background: #fafbfc; color: #667085; font-size: 12px; line-height: 1.8; }
.error-state strong { color: #1f2937; font-size: 17px; }
.error-state p { margin: 10px 0 20px; color: #667085; font-size: 14px; }
.error-actions { display: flex; flex-wrap: wrap; justify-content: center; gap: 10px; }
</style>
