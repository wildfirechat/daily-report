<template>
    <main class="page report-list-page">
        <header class="page-header">
            <div class="page-heading">
                <button class="back-link" type="button" @click="goHome">‹ 首页</button>
                <h1>{{ pageTitle }}</h1>
                <p>{{ pageSubtitle }}</p>
            </div>
            <button v-if="isMine" class="primary-button write-button" type="button" @click="writeReport">写日报</button>
        </header>

        <form class="filters" @submit.prevent="applyFilters">
            <input v-model.trim="filters.keyword" type="search" maxlength="100"
                   :placeholder="searchPlaceholder" aria-label="搜索日报">
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

        <p v-if="filterError" class="error-banner" role="alert">
            {{ filterError }}
            <button type="button" @click="filterError = ''">知道了</button>
        </p>

        <div id="report-panel">
            <section v-if="loading" class="report-list" aria-busy="true" aria-label="正在加载日报">
                <div v-for="n in 3" :key="n" class="report-card skeleton-card" aria-hidden="true">
                    <span class="skeleton-line skeleton-title"></span>
                    <span class="skeleton-line skeleton-meta"></span>
                    <span class="skeleton-line"></span>
                    <span class="skeleton-line skeleton-short"></span>
                </div>
            </section>

            <section v-else-if="listError" class="state-view error-state" role="alert">
                <strong>日报加载失败</strong>
                <p>{{ listError }}</p>
                <button class="primary-button" type="button" @click="loadReports(false)">重新加载</button>
            </section>

            <section v-else-if="!reports.length" class="state-view empty-view">
                <strong>{{ emptyTitle }}</strong>
                <p>{{ emptyDescription }}</p>
                <button v-if="isMine" class="primary-button" type="button" @click="writeReport">写第一篇日报</button>
                <button v-else class="secondary-button" type="button" @click="goHome">返回首页</button>
            </section>

            <section v-else class="report-list">
                <p class="list-summary">{{ resultSummary }}</p>
                <article v-for="report in reports" :key="`${report.userId}-${report.day}`"
                         class="report-card" role="button" tabindex="0"
                         :aria-label="`查看${report.title || '日报'}，${formatDate(report.day)}`"
                         @click="showReportDetail(report)"
                         @keydown.enter.prevent="showReportDetail(report)"
                         @keydown.space.prevent="showReportDetail(report)">
                    <div class="report-card-header">
                        <div>
                            <h2>{{ report.title || '日报' }}</h2>
                            <p>{{ formatDate(report.day) }}</p>
                        </div>
                        <span v-if="!isMine" class="author-badge">{{ report.displayName || report.userId }}</span>
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
                        <span v-if="isMine">
                            汇报对象 {{ (report.reportTo || []).length }} 人<span v-if="report.ccTo && report.ccTo.length"> · 抄送 {{ report.ccTo.length }} 人</span>
                        </span>
                        <span v-else>提交人：{{ report.displayName || report.userId }}</span>
                        <span v-if="report.updatedAt">更新于 {{ formatDateTime(report.updatedAt) }}</span>
                    </footer>
                </article>
            </section>

            <div v-if="reports.length" class="pagination">
                <p v-if="appendError" class="append-error" role="alert">
                    {{ appendError }}
                    <button type="button" @click="loadReports(true)">重试</button>
                </p>
                <button v-if="hasMore" class="secondary-button" type="button"
                        :disabled="loadingMore" @click="loadReports(true)">
                    {{ loadingMore ? '加载中...' : '加载更多' }}
                </button>
                <p v-else>已加载 {{ reports.length }} 篇，没有更多了</p>
            </div>
        </div>
    </main>
</template>

<script>
import api from "@/api/api";
import {formatDate, formatDateTime, fromDateInput} from '@/utils/date'
import {openPage} from '@/utils/navigation'

export default {
    name: "ReportListView",
    props: {
        account: {type: String, required: true},
        mode: {type: String, default: 'mine'}
    },
    data() {
        return {
            reports: [],
            filters: {keyword: '', startDate: '', endDate: ''},
            pageSize: 10,
            hasMore: false,
            nextCursorDay: null,
            nextCursorUpdatedAt: null,
            nextCursorUserId: null,
            loading: false,
            loadingMore: false,
            listError: '',
            filterError: '',
            appendError: '',
            requestId: 0
        }
    },
    computed: {
        isMine() {
            return this.mode === 'mine'
        },
        pageTitle() {
            return this.isMine ? '我的日报' : '汇报箱'
        },
        pageSubtitle() {
            return this.isMine ? '查看和回顾你的工作进展' : '查看同事发送给你的日报'
        },
        emptyTitle() {
            return this.isMine ? '还没有日报' : '汇报箱还是空的'
        },
        emptyDescription() {
            return this.isMine ? '提交后可以随时查看和修改。' : '同事发给你的日报会出现在这里。'
        },
        searchPlaceholder() {
            return this.isMine ? '搜索标题或内容' : '搜索标题、内容或提交人'
        },
        resultSummary() {
            if (!this.reports.length) {
                return ''
            }
            return this.hasMore ? `已加载 ${this.reports.length} 篇` : `共 ${this.reports.length} 篇`
        }
    },
    created() {
        this.loadReports(false)
    },
    methods: {
        formatDate,
        formatDateTime,
        queryFilters() {
            return {
                keyword: this.filters.keyword || undefined,
                startDay: fromDateInput(this.filters.startDate) || undefined,
                endDay: fromDateInput(this.filters.endDate) || undefined
            }
        },
        async loadReports(append) {
            if (!append && this.filters.startDate && this.filters.endDate
                && this.filters.startDate > this.filters.endDate) {
                this.filterError = '开始日期不能晚于结束日期'
                return
            }
            this.filterError = ''
            const currentRequest = ++this.requestId
            const filters = this.queryFilters()
            if (append) {
                filters.cursorDay = this.nextCursorDay
                filters.cursorUserId = this.nextCursorUserId || undefined
                if (!this.isMine) {
                    filters.cursorUpdatedAt = this.nextCursorUpdatedAt || this.nextCursorDay
                }
                this.loadingMore = true
                this.appendError = ''
            } else {
                this.loading = true
                this.listError = ''
                this.appendError = ''
                this.reports = []
                this.hasMore = false
                this.nextCursorDay = null
                this.nextCursorUpdatedAt = null
                this.nextCursorUserId = null
            }
            try {
                const request = this.isMine ? api.getReportFeed : api.getReceivedFeed
                const page = await request(this.pageSize, filters)
                if (currentRequest !== this.requestId) return
                this.reports = append ? this.reports.concat(page.items) : page.items
                this.nextCursorDay = page.nextCursorDay
                this.nextCursorUpdatedAt = page.nextCursorUpdatedAt || null
                this.nextCursorUserId = page.nextCursorUserId
                this.hasMore = page.hasMore
            } catch (e) {
                if (currentRequest !== this.requestId) return
                if (append) {
                    this.appendError = e.message || '加载更多失败，请稍后重试'
                } else {
                    this.listError = e.message || '日报加载失败，请稍后重试'
                }
            } finally {
                if (currentRequest === this.requestId) {
                    this.loading = false
                    this.loadingMore = false
                }
            }
        },
        applyFilters() {
            this.loadReports(false)
        },
        resetFilters() {
            this.filters = {keyword: '', startDate: '', endDate: ''}
            this.filterError = ''
            this.loadReports(false)
        },
        writeReport() {
            openPage('newReport.html?from=mine')
        },
        goHome() {
            openPage('index.html')
        },
        showReportDetail(report) {
            openPage(`report.html?userId=${encodeURIComponent(report.userId)}&day=${report.day}&from=${this.mode}`)
        }
    }
}
</script>

<style scoped>
.report-list-page { padding-top: 20px; }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 18px; }
.page-heading { min-width: 0; text-align: left; }
.back-link { min-height: 44px; padding: 0; border: 0; background: transparent; color: #667085; font-size: 13px; cursor: pointer; }
.back-link:focus-visible { outline: 3px solid rgba(47, 111, 237, .28); outline-offset: 2px; border-radius: 6px; }
.page-header h1 { margin-top: 2px; color: #1f2937; font-size: 28px; letter-spacing: .5px; }
.page-header p { margin-top: 6px; color: #667085; font-size: 14px; }
.write-button { flex-shrink: 0; }
.filters { padding: 2px 0 10px; }
.filters input { width: 100%; box-sizing: border-box; }
.date-filters { display: flex; align-items: center; gap: 8px; margin-top: 10px; color: #667085; }
.date-filters input { min-width: 0; }
.filter-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 10px; }
.filter-actions button { padding: 8px 20px; }
.report-card { margin: 10px 0; padding: 18px; border: 1px solid #e4e7ec; border-radius: 12px; background: #fff; box-shadow: 0 3px 12px rgba(35, 48, 73, .03); cursor: pointer; transition: border-color .16s ease, box-shadow .16s ease, transform .16s ease; }
.report-card:hover { border-color: #c9d5e6; box-shadow: 0 8px 24px rgba(35, 48, 73, .08); transform: translateY(-1px); }
.report-card:active { background: #f8faff; transform: translateY(0); }
.report-card:focus-visible { outline: 3px solid rgba(47, 111, 237, .28); outline-offset: 2px; }
.report-card-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.report-card-header h2 { color: #1f2937; font-size: 17px; line-height: 1.45; }
.report-card-header p { margin-top: 5px; color: #667085; font-size: 12px; }
.author-badge { max-width: 40%; padding: 4px 9px; overflow: hidden; border-radius: 12px; background: #eef4ff; color: #1d4ed8; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.report-summary { display: flex; gap: 12px; margin-top: 16px; text-align: left; }
.report-summary label { flex: 0 0 60px; color: #475467; font-size: 13px; line-height: 22px; }
.report-summary p { display: -webkit-box; margin: 0; overflow: hidden; color: #344054; font-size: 14px; line-height: 22px; white-space: pre-wrap; -webkit-box-orient: vertical; -webkit-line-clamp: 3; }
.secondary-summary { margin-top: 8px; }
.report-card footer { display: flex; justify-content: space-between; gap: 10px; margin-top: 16px; padding-top: 10px; border-top: 1px solid #f0f2f6; color: #667085; font-size: 12px; line-height: 1.5; }
.list-summary { margin: 4px 0 2px; color: #667085; font-size: 12px; text-align: left; }
.pagination { padding: 14px 0 30px; color: #667085; font-size: 12px; }
.pagination button { width: 100%; }
.append-error { margin-bottom: 8px; color: #b42318; }
.append-error button { margin-left: 6px; border: 0; background: transparent; color: inherit; font-size: inherit; text-decoration: underline; cursor: pointer; }
.error-state strong { color: #1f2937; font-size: 17px; }
.error-state p { margin: 10px 0 20px; color: #667085; font-size: 14px; }
.skeleton-card { pointer-events: none; cursor: default; }
.skeleton-card:hover { border-color: #e4e7ec; box-shadow: 0 3px 12px rgba(35, 48, 73, .03); transform: none; }
.skeleton-line { display: block; width: 100%; height: 14px; margin-top: 14px; border-radius: 6px; background: linear-gradient(90deg, #eef1f5 25%, #f7f9fc 37%, #eef1f5 63%); background-size: 400% 100%; animation: skeleton-shimmer 1.3s ease infinite; }
.skeleton-title { width: 55%; height: 18px; margin-top: 0; }
.skeleton-meta { width: 30%; height: 12px; margin-top: 10px; }
.skeleton-short { width: 42%; }
@keyframes skeleton-shimmer {
    0% { background-position: 100% 50%; }
    100% { background-position: 0 50%; }
}
@media (max-width: 480px) {
    .page-header h1 { font-size: 26px; }
    .report-card footer { flex-direction: column; gap: 4px; }
    .date-filters { gap: 6px; }
}
@media (prefers-reduced-motion: reduce) {
    .report-card, .skeleton-line { transition: none; animation: none; }
}
</style>
