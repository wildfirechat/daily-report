<template>
    <main class="page report-list-page">
        <header class="page-header">
            <div>
                <h1>工作日报</h1>
                <p>记录进展，也让协作更清晰</p>
            </div>
            <button class="primary-button write-button" @click="writeReport">写日报</button>
        </header>

        <section class="stats-grid" aria-label="日报统计">
            <div class="stat-card"><span>今日状态</span><strong :class="{pending: !stats.todaySubmitted}">{{ stats.todaySubmitted ? '已提交' : '未提交' }}</strong></div>
            <div class="stat-card"><span>本月提交</span><strong>{{ stats.monthCount }}</strong></div>
            <div class="stat-card"><span>累计提交</span><strong>{{ stats.totalCount }}</strong></div>
            <div class="stat-card"><span>收到日报</span><strong>{{ stats.receivedCount }}</strong></div>
        </section>

        <nav class="tabs" aria-label="日报类型">
            <button :class="{active: activeTab === 'mine'}" @click="switchTab('mine')">我的日报</button>
            <button :class="{active: activeTab === 'received'}" @click="switchTab('received')">汇报箱</button>
        </nav>

        <form class="filters" @submit.prevent="applyFilters">
            <input v-model.trim="filters.keyword" type="search" maxlength="100" placeholder="搜索标题、内容或提交人" aria-label="搜索日报">
            <div class="date-filters">
                <input v-model="filters.startDate" type="date" aria-label="开始日期">
                <span>至</span>
                <input v-model="filters.endDate" type="date" aria-label="结束日期">
            </div>
            <div class="filter-actions">
                <button class="secondary-button" type="button" @click="resetFilters">重置</button>
                <button class="primary-button" type="submit">查询</button>
            </div>
        </form>

        <p v-if="errorMessage" class="error-banner">{{ errorMessage }} <button @click="loadReports(false)">重试</button></p>
        <section v-if="loading && !reports.length" class="state-view">正在加载日报...</section>
        <section v-else-if="!reports.length" class="state-view empty-view">
            <strong>{{ emptyTitle }}</strong>
            <p>{{ emptyDescription }}</p>
            <button v-if="activeTab === 'mine'" class="primary-button" @click="writeReport">写第一篇日报</button>
        </section>
        <section v-else class="report-list">
            <article v-for="report in reports" :key="`${report.userId}-${report.day}`" class="report-card" @click="showReportDetail(report)">
                <div class="report-card-header">
                    <div>
                        <h2>{{ report.title || '日报' }}</h2>
                        <p>{{ formatDate(report.day) }}</p>
                    </div>
                    <span v-if="activeTab === 'received'" class="author-badge">{{ report.displayName || report.userId }}</span>
                </div>
                <div class="report-summary">
                    <label>今日完成</label>
                    <p>{{ report.content }}</p>
                </div>
                <div v-if="report.tomorrowPlan" class="report-summary secondary-summary">
                    <label>明日计划</label>
                    <p>{{ report.tomorrowPlan }}</p>
                </div>
                <footer>
                    <span>{{ activeTab === 'mine' ? (report.displayName || account) : `提交人：${report.displayName || report.userId}` }}</span>
                    <span v-if="report.updatedAt">更新于 {{ formatDateTime(report.updatedAt) }}</span>
                </footer>
            </article>
        </section>

        <div v-if="reports.length" class="pagination">
            <button v-if="hasMore" class="secondary-button" :disabled="loading" @click="loadReports(true)">{{ loading ? '加载中...' : '加载更多' }}</button>
            <p v-else>已加载 {{ reports.length }} 篇，没有更多了</p>
        </div>
    </main>
</template>

<script>
import api from "@/api/api";
import {formatDate, formatDateTime, fromDateInput} from '@/utils/date'
import {openPage} from '@/utils/navigation'

export default {
    name: "ReportListView",
    props: {account: {type: String, required: true}},
    data() {
        return {
            activeTab: 'mine',
            reports: [],
            stats: {todaySubmitted: false, monthCount: 0, totalCount: 0, receivedCount: 0},
            filters: {keyword: '', startDate: '', endDate: ''},
            pageSize: 10,
            hasMore: false,
            nextCursorDay: null,
            nextCursorUserId: null,
            loading: false,
            errorMessage: '',
            requestId: 0
        }
    },
    computed: {
        emptyTitle() {
            return this.activeTab === 'mine' ? '还没有日报' : '汇报箱还是空的'
        },
        emptyDescription() {
            return this.activeTab === 'mine' ? '提交后可以随时查看和修改。' : '同事发给你的日报会出现在这里。'
        }
    },
    created() {
        this.loadStats()
        this.loadReports(false)
    },
    methods: {
        formatDate,
        formatDateTime,
        async loadStats() {
            try {
                this.stats = await api.getReportStats()
            } catch (e) {
                console.error('Load report stats failed', e)
            }
        },
        queryFilters() {
            return {
                keyword: this.filters.keyword || undefined,
                startDay: fromDateInput(this.filters.startDate) || undefined,
                endDay: fromDateInput(this.filters.endDate) || undefined
            }
        },
        async loadReports(append) {
            if (this.filters.startDate && this.filters.endDate && this.filters.startDate > this.filters.endDate) {
                this.errorMessage = '开始日期不能晚于结束日期'
                return
            }
            const currentRequest = ++this.requestId
            const filters = this.queryFilters()
            if (append) {
                filters.cursorDay = this.nextCursorDay
                filters.cursorUserId = this.nextCursorUserId || undefined
            }
            this.loading = true
            this.errorMessage = ''
            try {
                const request = this.activeTab === 'mine' ? api.getReportFeed : api.getReceivedFeed
                const page = await request(this.pageSize, filters)
                if (currentRequest !== this.requestId) return
                this.reports = append ? this.reports.concat(page.items) : page.items
                this.nextCursorDay = page.nextCursorDay
                this.nextCursorUserId = page.nextCursorUserId
                this.hasMore = page.hasMore
            } catch (e) {
                if (currentRequest === this.requestId) {
                    this.errorMessage = e.message || '日报加载失败，请稍后重试'
                }
            } finally {
                if (currentRequest === this.requestId) this.loading = false
            }
        },
        switchTab(tab) {
            if (this.activeTab === tab) return
            this.activeTab = tab
            this.reports = []
            this.loadReports(false)
        },
        applyFilters() {
            this.loadReports(false)
        },
        resetFilters() {
            this.filters = {keyword: '', startDate: '', endDate: ''}
            this.loadReports(false)
        },
        writeReport() {
            openPage('newReport.html')
        },
        showReportDetail(report) {
            openPage(`report.html?userId=${encodeURIComponent(report.userId)}&day=${report.day}`)
        }
    }
}
</script>

<style scoped>
.report-list-page { padding-top: 24px; }
.page-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 22px; }
.page-header h1 { font-size: 26px; letter-spacing: 1px; }
.page-header p { margin-top: 6px; color: #7b8494; font-size: 13px; }
.write-button { flex-shrink: 0; }
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.stat-card { padding: 16px 12px; border: 1px solid #edf0f5; border-radius: 12px; background: #fff; text-align: left; }
.stat-card span { display: block; color: #8a93a3; font-size: 12px; }
.stat-card strong { display: block; margin-top: 8px; color: #2f6fed; font-size: 22px; }
.stat-card strong.pending { color: #e68a21; font-size: 18px; }
.tabs { display: flex; margin-top: 24px; border-bottom: 1px solid #e7ebf1; }
.tabs button { flex: 1; padding: 13px; border: 0; border-bottom: 2px solid transparent; background: transparent; color: #7c8594; font-size: 15px; cursor: pointer; }
.tabs button.active { border-bottom-color: #2f6fed; color: #2f6fed; font-weight: 600; }
.filters { padding: 16px 0 10px; }
.filters input { width: 100%; box-sizing: border-box; }
.date-filters { display: flex; align-items: center; gap: 8px; margin-top: 10px; color: #98a0ae; }
.date-filters input { min-width: 0; }
.filter-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 10px; }
.filter-actions button { padding: 8px 20px; }
.report-card { margin: 10px 0; padding: 18px; border: 1px solid #e9edf3; border-radius: 12px; background: #fff; box-shadow: 0 3px 12px rgba(35, 48, 73, .03); cursor: pointer; }
.report-card:active { background: #f8faff; }
.report-card-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.report-card-header h2 { font-size: 17px; }
.report-card-header p { margin-top: 5px; color: #8b94a4; font-size: 12px; }
.author-badge { max-width: 40%; padding: 4px 9px; overflow: hidden; border-radius: 12px; background: #edf3ff; color: #2f6fed; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.report-summary { display: flex; gap: 12px; margin-top: 16px; text-align: left; }
.report-summary label { flex: 0 0 60px; color: #697386; font-size: 13px; line-height: 22px; }
.report-summary p { display: -webkit-box; margin: 0; overflow: hidden; color: #303a4d; font-size: 14px; line-height: 22px; white-space: pre-wrap; -webkit-box-orient: vertical; -webkit-line-clamp: 3; }
.secondary-summary { margin-top: 8px; }
.report-card footer { display: flex; justify-content: space-between; gap: 10px; margin-top: 16px; padding-top: 10px; border-top: 1px solid #f0f2f6; color: #a0a7b3; font-size: 11px; }
.pagination { padding: 14px 0 30px; color: #9aa2af; font-size: 12px; }
.pagination button { width: 100%; }
@media (max-width: 480px) {
    .stats-grid { grid-template-columns: repeat(2, 1fr); }
    .stat-card { padding: 12px; }
    .report-card footer { flex-direction: column; }
}
</style>
