<template>
    <main class="page editor-page">
        <header class="editor-header">
            <button class="text-button" type="button" @click="cancel">返回</button>
            <h1>{{ isEditing ? '编辑日报' : '写日报' }}</h1>
            <span class="header-spacer"></span>
        </header>
        <section v-if="loading" class="state-view" aria-busy="true">正在加载日报...</section>
        <form v-else @submit.prevent="submitReport">
            <p v-if="draftRestored" class="draft-tip">已恢复上次未提交的草稿 <button type="button" @click="discardDraft">清除草稿</button></p>
            <p v-if="errorMessage" class="error-banner" role="alert">{{ errorMessage }}</p>

            <section class="form-card">
                <label class="field-label" for="report-date">汇报日期</label>
                <input id="report-date" v-model="reportDate" type="date" :max="today" required @change="changeDate">
                <label class="field-label title-label" for="report-title">标题</label>
                <input id="report-title" v-model="report.title" maxlength="64" placeholder="日报" @input="saveDraft">
            </section>

            <section class="form-card">
                <div class="field-heading"><label for="report-content">今日工作总结 <em>*</em></label><span>{{ report.content.length }}/2048</span></div>
                <textarea id="report-content" v-model="report.content" maxlength="2048" rows="6" placeholder="完成了什么？有哪些进展和结果？" @input="saveDraft"></textarea>
            </section>
            <section class="form-card">
                <div class="field-heading"><label for="report-plan">明日工作计划</label><span>{{ report.tomorrowPlan.length }}/2048</span></div>
                <textarea id="report-plan" v-model="report.tomorrowPlan" maxlength="2048" rows="4" placeholder="明天准备做什么？" @input="saveDraft"></textarea>
            </section>
            <section class="form-card">
                <div class="field-heading"><label for="report-requirement">需要帮助与协调</label><span>{{ report.requirement.length }}/2048</span></div>
                <textarea id="report-requirement" v-model="report.requirement" maxlength="2048" rows="4" placeholder="遇到的问题、风险或需要同事协助的事项" @input="saveDraft"></textarea>
            </section>

            <section class="form-card recipients-card">
                <button type="button" class="recipient-row" @click="chooseContacts('reportTo')">
                    <span>汇报对象 <em>*</em></span><strong>{{ reportToDesc }}<i>›</i></strong>
                </button>
                <button type="button" class="recipient-row" @click="chooseContacts('ccTo')">
                    <span>抄送给</span><strong>{{ ccToDesc }}<i>›</i></strong>
                </button>
            </section>

            <button class="primary-button submit-button" type="submit" :disabled="submitting">{{ submitting ? '正在提交...' : (isEditing ? '保存修改' : '提交日报') }}</button>
            <p class="submit-note">提交后将通过野火消息通知汇报对象和抄送对象</p>
        </form>
    </main>
</template>

<script>
import wf from "@/jssdk/wf";
import Report from "@/model/report";
import api from "@/api/api";
import {fromDateInput, toDateInput} from '@/utils/date'
import {openPage} from '@/utils/navigation'
import {queueToast} from '@/utils/toast'

export default {
    name: 'NewReportView',
    props: {account: {type: String, required: true}},
    data() {
        return {
            report: new Report(this.account),
            reportDate: toDateInput(Date.now()),
            today: toDateInput(Date.now()),
            userTargets: [],
            ccTargets: [],
            isEditing: false,
            loading: false,
            submitting: false,
            errorMessage: '',
            draftRestored: false
        }
    },
    computed: {
        draftKey() {
            return `daily-report:draft:${this.account}`
        },
        reportToDesc() {
            return this.targetDescription(this.userTargets, this.report.reportTo, '请选择')
        },
        ccToDesc() {
            return this.targetDescription(this.ccTargets, this.report.ccTo, '选填')
        }
    },
    created() {
        const day = Number(new URLSearchParams(window.location.search).get('day'))
        if (day > 0) {
            this.isEditing = true
            this.report.day = day
            this.reportDate = toDateInput(day)
            this.loadReport(day)
        } else {
            this.restoreDraft()
        }
    },
    methods: {
        async loadReport(day) {
            this.loading = true
            try {
                const report = await api.getReport(this.account, day)
                Object.assign(this.report, report)
                this.report.reportTo = report.reportTo || []
                this.report.ccTo = report.ccTo || []
                this.reportDate = toDateInput(report.day)
            } catch (e) {
                this.errorMessage = e.message || '日报加载失败'
            } finally {
                this.loading = false
            }
        },
        chooseContacts(type) {
            wf.biz.chooseContacts({max: 20}, (userInfos) => {
                const targets = type === 'reportTo' ? this.userTargets : this.ccTargets
                targets.splice(0, targets.length, ...userInfos)
                this.report[type] = userInfos.map(user => user.uid)
                this.saveDraft()
            }, (err) => {
                this.errorMessage = '选择联系人失败：' + err
            })
        },
        targetDescription(targets, selected, placeholder) {
            if (targets.length) return targets.map(user => user.displayName).join('、')
            if (selected && selected.length) return `已选择 ${selected.length} 人`
            return placeholder
        },
        changeDate() {
            this.report.day = fromDateInput(this.reportDate)
            this.saveDraft()
        },
        async submitReport() {
            if (this.submitting) return
            this.errorMessage = ''
            this.report.day = fromDateInput(this.reportDate)
            if (!this.report.day || this.reportDate > this.today) {
                this.errorMessage = '请选择有效的汇报日期，不能选择未来日期'
                return
            }
            const validation = this.report.valid()
            if (validation.code !== 0) {
                this.errorMessage = validation.msg
                return
            }
            this.submitting = true
            try {
                const saved = await api.report(this.report)
                sessionStorage.removeItem(this.draftKey)
                queueToast(this.isEditing ? '日报已更新' : '日报已提交')
                openPage(`report.html?userId=${encodeURIComponent(saved.userId)}&day=${saved.day}&from=mine`)
            } catch (e) {
                this.errorMessage = e.message || '日报提交失败，请稍后重试'
                this.submitting = false
            }
        },
        saveDraft() {
            if (this.isEditing) return
            try {
                sessionStorage.setItem(this.draftKey, JSON.stringify({report: this.report, reportDate: this.reportDate}))
            } catch (e) {
                console.warn('Save draft failed', e)
            }
        },
        restoreDraft() {
            try {
                const draft = JSON.parse(sessionStorage.getItem(this.draftKey))
                if (draft && draft.report) {
                    Object.assign(this.report, draft.report)
                    this.reportDate = draft.reportDate || toDateInput(this.report.day)
                    this.draftRestored = true
                }
            } catch (e) {
                sessionStorage.removeItem(this.draftKey)
            }
        },
        discardDraft() {
            sessionStorage.removeItem(this.draftKey)
            this.report = new Report(this.account)
            this.reportDate = toDateInput(Date.now())
            this.userTargets = []
            this.ccTargets = []
            this.draftRestored = false
        },
        cancel() {
            const from = new URLSearchParams(window.location.search).get('from')
            openPage(from === 'home' ? 'index.html' : 'myReports.html')
        }
    }
}
</script>

<style scoped>
.editor-page { padding-top: 12px; }
.editor-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.editor-header h1 { color: #1f2937; font-size: 20px; }
.text-button { min-height: 44px; padding: 8px 0; border: 0; background: transparent; color: #667085; cursor: pointer; }
.header-spacer { width: 32px; }
.form-card { margin-bottom: 12px; padding: 16px; border: 1px solid #e4e7ec; border-radius: 12px; background: #fff; text-align: left; }
.field-label, .field-heading label { color: #344054; font-size: 14px; font-weight: 600; }
.title-label { display: block; margin-top: 16px; }
.form-card input { margin-top: 9px; }
.field-heading { display: flex; align-items: center; justify-content: space-between; }
.field-heading span { color: #667085; font-size: 12px; font-variant-numeric: tabular-nums; }
em { color: #d92d20; font-style: normal; }
.form-card textarea { box-sizing: border-box; width: 100%; min-height: 96px; margin-top: 10px; padding: 0; border: 0; outline: none; background: transparent; color: #344054; font: inherit; font-size: 15px; line-height: 1.7; resize: none; }
.form-card textarea::placeholder { color: #98a2b3; }
.recipients-card { padding: 0 16px; }
.recipient-row { display: flex; align-items: center; justify-content: space-between; width: 100%; min-height: 58px; padding: 0; border: 0; border-bottom: 1px solid #edf0f4; background: transparent; color: #344054; font-size: 14px; text-align: left; cursor: pointer; }
.recipient-row:last-child { border-bottom: 0; }
.recipient-row strong { max-width: 65%; overflow: hidden; color: #475467; font-size: 13px; font-weight: 400; text-overflow: ellipsis; white-space: nowrap; }
.recipient-row i { margin-left: 10px; color: #667085; font-size: 22px; font-style: normal; vertical-align: middle; }
.submit-button { width: 100%; min-height: 48px; margin-top: 10px; padding: 13px; }
.submit-note { margin: 12px 0 28px; color: #667085; font-size: 12px; line-height: 1.6; }
.draft-tip { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 12px; padding: 8px 12px; border: 1px solid #fedf89; border-radius: 8px; background: #fffaeb; color: #93370d; font-size: 13px; text-align: left; }
.draft-tip button { min-height: 44px; border: 0; background: transparent; color: #93370d; font-size: 13px; text-decoration: underline; cursor: pointer; }
</style>
