<template>
    <main class="page home-page">
        <header class="home-header">
            <p class="home-kicker">{{ todayLabel }}</p>
            <h1>工作日报</h1>
            <p class="home-subtitle">记录进展，也让协作更清晰</p>
        </header>

        <section class="stats-grid" aria-label="日报统计">
            <div class="stat-card">
                <span>今日状态</span>
                <strong :class="todayStatusClass">{{ statsUnavailable ? '—' : (stats.todaySubmitted ? '已提交' : '未提交') }}</strong>
            </div>
            <div class="stat-card"><span>本月提交</span><strong>{{ statsUnavailable ? '—' : stats.monthCount }}</strong></div>
            <div class="stat-card"><span>累计提交</span><strong>{{ statsUnavailable ? '—' : stats.totalCount }}</strong></div>
            <div class="stat-card"><span>收到日报</span><strong>{{ statsUnavailable ? '—' : stats.receivedCount }}</strong></div>
        </section>
        <p v-if="statsError" class="stats-error" role="status">
            统计暂不可用
            <button type="button" @click="loadStats">重试</button>
        </p>

        <section class="home-actions" aria-label="快捷入口">
            <button class="action-card action-primary" type="button" @click="go('newReport.html?from=home')">
                <span class="action-icon" aria-hidden="true">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                         stroke-linecap="round" stroke-linejoin="round">
                        <path d="M12 20h9"/>
                        <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z"/>
                    </svg>
                </span>
                <span class="action-copy">
                    <strong>写日报</strong>
                    <small>{{ stats.todaySubmitted ? '今日已提交，可继续更新' : '记录今天的工作进展' }}</small>
                </span>
                <span class="action-chevron" aria-hidden="true">›</span>
            </button>

            <div class="action-grid">
                <button class="action-card" type="button" @click="go('myReports.html')">
                    <span class="action-icon" aria-hidden="true">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                             stroke-linecap="round" stroke-linejoin="round">
                            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8Z"/>
                            <path d="M14 2v6h6"/>
                            <path d="M8 13h8"/>
                            <path d="M8 17h5"/>
                        </svg>
                    </span>
                    <span class="action-copy">
                        <strong>我的日报</strong>
                        <small>查看、编辑历史日报</small>
                    </span>
                    <span class="action-meta">{{ statsUnavailable ? '—' : stats.totalCount }} 篇</span>
                    <span class="action-chevron" aria-hidden="true">›</span>
                </button>

                <button class="action-card" type="button" @click="go('inbox.html')">
                    <span class="action-icon" aria-hidden="true">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                             stroke-linecap="round" stroke-linejoin="round">
                            <path d="M22 12h-6l-2 3h-4l-2-3H2"/>
                            <path d="M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11Z"/>
                        </svg>
                    </span>
                    <span class="action-copy">
                        <strong>汇报箱</strong>
                        <small>查看同事发来的日报</small>
                    </span>
                    <span class="action-meta">{{ statsUnavailable ? '—' : stats.receivedCount }} 篇</span>
                    <span class="action-chevron" aria-hidden="true">›</span>
                </button>
            </div>
        </section>
    </main>
</template>

<script>
import api from "@/api/api";
import {formatDate} from '@/utils/date'
import {openPage} from '@/utils/navigation'

export default {
    name: "HomeView",
    data() {
        return {
            stats: {todaySubmitted: false, monthCount: 0, totalCount: 0, receivedCount: 0},
            statsError: false,
            statsLoading: true
        }
    },
    computed: {
        statsUnavailable() {
            return this.statsError || this.statsLoading
        },
        todayLabel() {
            return formatDate(Date.now())
        },
        todayStatusClass() {
            if (this.statsUnavailable) {
                return ''
            }
            return this.stats.todaySubmitted ? 'status-submitted' : 'pending'
        }
    },
    created() {
        this.loadStats()
    },
    methods: {
        async loadStats() {
            this.statsError = false
            this.statsLoading = true
            try {
                this.stats = await api.getReportStats()
            } catch (e) {
                this.statsError = true
            } finally {
                this.statsLoading = false
            }
        },
        go(page) {
            openPage(page)
        }
    }
}
</script>

<style scoped>
.home-page { padding-top: 28px; }
.home-header { text-align: left; }
.home-kicker { color: #2f6fed; font-size: 11px; font-weight: 600; letter-spacing: 1.4px; }
.home-header h1 { margin-top: 7px; color: #1f2937; font-size: 30px; letter-spacing: .5px; }
.home-subtitle { margin-top: 7px; color: #667085; font-size: 14px; }
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin-top: 24px; }
.stat-card { padding: 14px 14px 13px; border: 1px solid #e4e7ec; border-radius: 12px; background: #fff; text-align: left; }
.stat-card span { display: block; color: #667085; font-size: 12px; }
.stat-card strong { display: block; margin-top: 7px; color: #2f6fed; font-size: 22px; font-variant-numeric: tabular-nums; }
.stat-card strong.status-submitted { color: #027a48; }
.stat-card strong.pending { color: #b54708; font-size: 18px; }
.stats-error { margin-top: 8px; color: #b42318; font-size: 12px; text-align: left; }
.stats-error button { margin-left: 6px; border: 0; background: transparent; color: inherit; font-size: inherit; text-decoration: underline; cursor: pointer; }
.home-actions { display: grid; gap: 10px; margin-top: 22px; }
.action-grid { display: grid; gap: 10px; }
.action-card { display: flex; align-items: center; gap: 14px; width: 100%; min-height: 82px; padding: 16px; border: 1px solid #e4e7ec; border-radius: 14px; background: #fff; color: #1f2937; text-align: left; cursor: pointer; transition: border-color .16s ease, box-shadow .16s ease, transform .16s ease; }
.action-card:hover { border-color: #c9d5e6; box-shadow: 0 8px 24px rgba(35, 48, 73, .08); transform: translateY(-1px); }
.action-card:active { transform: translateY(0); }
.action-card:focus-visible { outline: 3px solid rgba(47, 111, 237, .28); outline-offset: 2px; }
.action-primary { border-color: #2f6fed; background: linear-gradient(135deg, #2f6fed, #1d4ed8); box-shadow: 0 8px 20px rgba(47, 111, 237, .18); color: #fff; }
.action-primary:hover { border-color: #1d4ed8; box-shadow: 0 10px 24px rgba(47, 111, 237, .24); }
.action-icon { display: inline-flex; align-items: center; justify-content: center; flex: 0 0 42px; width: 42px; height: 42px; border-radius: 12px; background: #eef4ff; color: #1d4ed8; }
.action-icon svg { width: 21px; height: 21px; }
.action-primary .action-icon { background: rgba(255, 255, 255, .16); color: #fff; }
.action-copy { display: flex; flex: 1; flex-direction: column; gap: 3px; min-width: 0; }
.action-copy strong { font-size: 16px; line-height: 1.4; }
.action-copy small { overflow: hidden; color: #667085; font-size: 12px; line-height: 1.5; text-overflow: ellipsis; white-space: nowrap; }
.action-primary .action-copy small { color: rgba(255, 255, 255, .78); }
.action-meta { flex: 0 0 auto; padding: 3px 8px; border-radius: 999px; background: #f2f4f7; color: #475467; font-size: 12px; white-space: nowrap; }
.action-chevron { flex: 0 0 auto; color: #98a2b3; font-size: 22px; line-height: 1; }
.action-primary .action-chevron { color: rgba(255, 255, 255, .8); }
@media (min-width: 560px) {
    .action-grid { grid-template-columns: 1fr 1fr; }
}
@media (max-width: 480px) {
    .home-page { padding-top: 20px; }
    .home-header h1 { font-size: 28px; }
    .stats-grid { grid-template-columns: repeat(2, 1fr); }
    .stat-card { padding: 12px; }
    .action-card { min-height: 76px; padding: 14px; gap: 12px; }
    .action-icon { flex-basis: 38px; width: 38px; height: 38px; }
    .action-copy strong { font-size: 15px; }
}
@media (prefers-reduced-motion: reduce) {
    .action-card { transition: none; }
}
</style>
