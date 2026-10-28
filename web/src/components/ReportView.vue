<template>
    <main class="page detail-page">
        <header class="detail-nav">
            <button class="text-button" @click="backToList">‹ 返回日报</button>
            <div v-if="report && isOwner" class="detail-actions">
                <button @click="editReport">编辑</button>
                <button class="danger-button" @click="deleteReport">删除</button>
            </div>
        </header>
        <section v-if="loading" class="state-view">正在加载日报...</section>
        <section v-else-if="errorMessage" class="state-view">
            <strong>{{ errorMessage }}</strong>
            <button class="secondary-button" @click="getReport">重新加载</button>
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
    </main>
</template>

<script>
import api from "@/api/api";
import {formatDate, formatDateTime} from '@/utils/date'
import {openPage} from '@/utils/navigation'

export default {
    name: "ReportView",
    props: {account: {type: String, required: true}},
    data() {
        return {report: null, loading: true, errorMessage: ''}
    },
    computed: {
        isOwner() {
            return this.report && this.report.userId === this.account
        },
        authorInitial() {
            const name = this.report.displayName || this.report.userId || '日'
            return name.substring(0, 1).toUpperCase()
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
            const params = new URLSearchParams(window.location.search)
            const userId = params.get('userId') || this.account
            const day = Number(params.get('day')) || Date.now()
            try {
                this.report = await api.getReport(userId, day)
            } catch (e) {
                this.report = null
                this.errorMessage = e.code === 16 ? '日报不存在或无权查看' : (e.message || '日报加载失败')
            } finally {
                this.loading = false
            }
        },
        editReport() {
            openPage(`newReport.html?day=${this.report.day}`)
        },
        async deleteReport() {
            if (!window.confirm('确定删除这篇日报吗？删除后无法恢复。')) return
            try {
                await api.deleteReport(this.report.day)
                openPage('index.html')
            } catch (e) {
                this.errorMessage = e.message || '删除失败，请稍后重试'
            }
        },
        backToList() {
            openPage('index.html')
        }
    }
}
</script>

<style scoped>
.detail-page { padding-top: 16px; }
.detail-nav { display: flex; align-items: center; justify-content: space-between; min-height: 40px; margin-bottom: 12px; }
.text-button, .detail-actions button { padding: 8px; border: 0; background: transparent; color: #667085; cursor: pointer; }
.detail-actions { display: flex; gap: 8px; }
.detail-actions button { color: #2f6fed; }
.detail-actions .danger-button { color: #d84d4d; }
.detail-card { overflow: hidden; border: 1px solid #e9edf3; border-radius: 14px; background: #fff; text-align: left; }
.detail-header { padding: 26px 22px 20px; background: linear-gradient(145deg, #f2f6ff, #fff); }
.eyebrow { color: #2f6fed; font-size: 11px; font-weight: 600; letter-spacing: 3px; }
.detail-header h1 { margin-top: 8px; font-size: 25px; }
.report-date { margin-top: 8px; color: #7e8899; font-size: 13px; }
.author-line { display: flex; align-items: center; gap: 9px; margin-top: 20px; font-size: 14px; }
.avatar { display: inline-flex; align-items: center; justify-content: center; width: 32px; height: 32px; border-radius: 50%; background: #2f6fed; color: #fff; }
.detail-section { padding: 22px; border-top: 1px solid #edf0f4; }
.detail-section h2 { color: #526077; font-size: 14px; }
.detail-section p { margin-top: 12px; color: #27344a; font-size: 15px; line-height: 1.85; overflow-wrap: anywhere; white-space: pre-wrap; }
.detail-section p.placeholder { color: #a4acb9; }
.detail-meta { padding: 16px 22px; border-top: 1px solid #edf0f4; background: #fafbfc; color: #929ba9; font-size: 11px; line-height: 1.8; }
.state-view .secondary-button { margin-top: 18px; }
</style>
