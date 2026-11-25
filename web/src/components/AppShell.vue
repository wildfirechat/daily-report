<template>
    <HomeView v-if="route.view === 'home'"
              :refresh-key="refreshKey"
              @navigate="navigate"/>
    <ReportListView v-else-if="route.view === 'mine' || route.view === 'received'"
                    :key="route.view"
                    :account="account"
                    :mode="route.view"
                    :refresh-key="refreshKey"
                    @navigate="navigate"/>
    <ReportView v-else-if="route.view === 'detail'"
                :account="account"
                :user-id="route.userId"
                :day="route.day"
                :from="route.from"
                :refresh-key="refreshKey"
                @navigate="navigate"/>
</template>

<script>
import HomeView from '@/components/HomeView'
import ReportListView from '@/components/ReportListView'
import ReportView from '@/components/ReportView'
import {SUBMITTED_KEY, consumeReportSubmitted} from '@/utils/navigation'

const VIEW_TITLES = {
    home: '工作日报',
    mine: '我的日报',
    received: '汇报箱',
    detail: '日报详情'
}

export default {
    name: 'AppShell',
    components: {HomeView, ReportListView, ReportView},
    props: {
        account: {type: String, required: true},
        initialView: {type: String, default: 'home'}
    },
    data() {
        return {
            route: this.buildInitialRoute(),
            refreshKey: 0
        }
    },
    created() {
        this.applyTitle()
        this.syncHistoryState(this.route, true)
    },
    mounted() {
        window.addEventListener('popstate', this.handlePopState)
        window.addEventListener('storage', this.handleStorage)
        window.addEventListener('focus', this.checkSubmittedReport)
        document.addEventListener('visibilitychange', this.handleVisibilityChange)
    },
    beforeUnmount() {
        window.removeEventListener('popstate', this.handlePopState)
        window.removeEventListener('storage', this.handleStorage)
        window.removeEventListener('focus', this.checkSubmittedReport)
        document.removeEventListener('visibilitychange', this.handleVisibilityChange)
    },
    methods: {
        buildInitialRoute() {
            if (this.initialView === 'detail') {
                const params = new URLSearchParams(window.location.search)
                return {
                    view: 'detail',
                    userId: params.get('userId') || this.account,
                    day: Number(params.get('day')) || Date.now(),
                    from: params.get('from') || ''
                }
            }
            return {view: this.initialView || 'home'}
        },
        normalizeRoute(next) {
            const view = (next && next.view) || 'home'
            if (view === 'detail') {
                return {
                    view: 'detail',
                    userId: next.userId || this.account,
                    day: Number(next.day) || Date.now(),
                    from: next.from || ''
                }
            }
            return {view}
        },
        applyTitle() {
            document.title = VIEW_TITLES[this.route.view] || '工作日报'
        },
        syncHistoryState(route, replace) {
            try {
                const state = {dailyReportRoute: {...route}}
                if (replace) {
                    window.history.replaceState(state, '', window.location.href)
                } else {
                    window.history.pushState(state, '', window.location.href)
                }
            } catch (e) {
                // History API may be restricted in some embedded WebViews.
            }
        },
        navigate(nextRoute, options = {}) {
            const target = this.normalizeRoute(nextRoute)
            const replace = Boolean(options && options.replace)
            this.route = target
            if (target.view === 'detail') {
                this.refreshKey += 1
            }
            this.applyTitle()
            this.syncHistoryState(target, replace)
            window.scrollTo(0, 0)
        },
        handlePopState(event) {
            const saved = event && event.state && event.state.dailyReportRoute
            if (saved && saved.view) {
                this.route = this.normalizeRoute(saved)
                this.applyTitle()
            }
        },
        handleStorage(event) {
            if (event && event.key === SUBMITTED_KEY && event.newValue) {
                this.checkSubmittedReport()
            }
        },
        handleVisibilityChange() {
            if (document.visibilityState === 'visible') {
                this.checkSubmittedReport()
            }
        },
        checkSubmittedReport() {
            const submitted = consumeReportSubmitted()
            if (!submitted || !submitted.day) {
                return
            }
            this.refreshKey += 1
        }
    }
}
</script>
