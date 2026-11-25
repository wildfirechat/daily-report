<template>
    <main class="page editor-page">
        <header class="editor-header">
            <h1>{{ isEditing ? '编辑日报' : '写日报' }}</h1>
        </header>
        <section v-if="loading" class="state-view" aria-busy="true">正在加载日报...</section>
        <section v-else-if="loadError" class="state-view error-state" role="alert">
            <strong>{{ loadError }}</strong>
            <p>请检查网络后重试，或关闭当前窗口。</p>
            <div class="error-actions">
                <button class="primary-button" type="button" @click="loadReport(originalDay)">重新加载</button>
                <button class="secondary-button" type="button" @click="cancel">关闭</button>
            </div>
        </section>
        <form v-else class="editor-form" @submit.prevent="submitReport">
            <p v-if="existingReport" class="draft-tip" role="status">
                <span>该日期已提交过日报，直接提交将覆盖原日报</span>
                <button type="button" @click="loadExistingReport">加载已有日报</button>
            </p>
            <p v-if="draftRestored" class="draft-tip">
                <span>已恢复上次未提交的草稿</span>
                <button type="button" @click="discardDraft">清除草稿</button>
            </p>
            <p v-if="errorMessage" class="error-banner" role="alert">{{ errorMessage }}</p>

            <section class="form-card meta-card">
                <div class="meta-grid">
                    <div class="field-group">
                        <label class="field-label" for="report-date">汇报日期</label>
                        <input id="report-date" v-model="reportDate" type="date" :max="today" :disabled="isEditing" required @change="changeDate">
                        <p v-if="isEditing" class="field-hint">编辑模式下日期不可修改</p>
                    </div>
                    <div class="field-group">
                        <label class="field-label" for="report-title">标题</label>
                        <input id="report-title" v-model="report.title" maxlength="64" placeholder="请输入日报标题（默认：日报）" @input="saveDraft">
                    </div>
                </div>
            </section>

            <section class="form-card">
                <div class="field-heading">
                    <label for="report-content">今日工作总结 <em>*</em></label>
                    <span class="char-count">{{ (report.content || '').length }}/2048</span>
                </div>
                <textarea id="report-content" class="editor-textarea editor-textarea-lg" v-model="report.content" maxlength="2048" rows="6" placeholder="完成了什么？有哪些进展和结果？" @input="saveDraft"></textarea>
            </section>
            <section class="form-card">
                <div class="field-heading">
                    <label for="report-plan">明日工作计划</label>
                    <span class="char-count">{{ (report.tomorrowPlan || '').length }}/2048</span>
                </div>
                <textarea id="report-plan" class="editor-textarea" v-model="report.tomorrowPlan" maxlength="2048" rows="4" placeholder="明天准备做什么？" @input="saveDraft"></textarea>
            </section>
            <section class="form-card">
                <div class="field-heading">
                    <label for="report-requirement">需要帮助与协调</label>
                    <span class="char-count">{{ (report.requirement || '').length }}/2048</span>
                </div>
                <textarea id="report-requirement" class="editor-textarea" v-model="report.requirement" maxlength="2048" rows="4" placeholder="遇到的问题、风险或需要同事协助的事项" @input="saveDraft"></textarea>
            </section>

            <section class="form-card recipients-card">
                <button type="button" class="recipient-row" @click="chooseContacts('reportTo')">
                    <span class="recipient-label">汇报对象 <em>*</em></span>
                    <strong :class="{'is-placeholder': !hasReportTo}">{{ reportToDesc }}<i>›</i></strong>
                </button>
                <button type="button" class="recipient-row" @click="chooseContacts('ccTo')">
                    <span class="recipient-label">抄送给</span>
                    <strong :class="{'is-placeholder': !hasCcTo}">{{ ccToDesc }}<i>›</i></strong>
                </button>
            </section>

            <div class="submit-section">
                <button class="primary-button submit-button" type="submit" :disabled="submitting">{{ submitting ? '正在提交...' : (isEditing ? '保存修改' : '提交日报') }}</button>
                <p class="submit-note">提交后将通过野火消息通知汇报对象和抄送对象</p>
            </div>
        </form>
    </main>
</template>

<script>
import wf from "@/jssdk/wf";
import Report from "@/model/report";
import api from "@/api/api";
import {fromDateInput, toDateInput} from '@/utils/date'
import {closeEditorWindow, notifyReportSubmitted} from '@/utils/navigation'
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
            originalDay: null,
            loading: false,
            loadError: '',
            submitting: false,
            errorMessage: '',
            draftRestored: false,
            existingReport: null,
            existingCheckId: 0
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
        },
        hasReportTo() {
            return Boolean((this.userTargets && this.userTargets.length) || (this.report.reportTo && this.report.reportTo.length))
        },
        hasCcTo() {
            return Boolean((this.ccTargets && this.ccTargets.length) || (this.report.ccTo && this.report.ccTo.length))
        }
    },
    created() {
        const day = Number(new URLSearchParams(window.location.search).get('day'))
        if (day > 0) {
            this.isEditing = true
            this.originalDay = day
            this.report.day = day
            this.reportDate = toDateInput(day)
            this.loadReport(day)
        } else {
            this.restoreDraft()
            this.checkExistingReport()
        }
    },
    methods: {
        applyLoadedReport(report) {
            Object.assign(this.report, report)
            this.report.title = report.title || ''
            this.report.content = report.content || ''
            this.report.tomorrowPlan = report.tomorrowPlan || ''
            this.report.requirement = report.requirement || ''
            this.report.reportTo = Array.isArray(report.reportTo) ? report.reportTo : []
            this.report.ccTo = Array.isArray(report.ccTo) ? report.ccTo : []
            this.reportDate = toDateInput(report.day)
            this.originalDay = report.day
        },
        async loadReport(day) {
            this.loading = true
            this.loadError = ''
            this.errorMessage = ''
            try {
                const report = await api.getReport(this.account, day)
                this.applyLoadedReport(report)
            } catch (e) {
                this.loadError = e.code === 16 ? '日报不存在或无权编辑' : (e.message || '日报加载失败')
            } finally {
                this.loading = false
            }
        },
        async checkExistingReport() {
            if (this.isEditing) {
                this.existingReport = null
                return
            }
            const day = fromDateInput(this.reportDate)
            if (!day) {
                this.existingReport = null
                return
            }
            const checkId = ++this.existingCheckId
            try {
                const report = await api.getReport(this.account, day)
                if (checkId !== this.existingCheckId || this.isEditing) return
                this.existingReport = report || null
            } catch (e) {
                if (checkId !== this.existingCheckId) return
                this.existingReport = null
            }
        },
        loadExistingReport() {
            if (!this.existingReport) return
            this.isEditing = true
            this.draftRestored = false
            this.userTargets = []
            this.ccTargets = []
            this.applyLoadedReport(this.existingReport)
            this.existingReport = null
            this.errorMessage = ''
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
            if (this.isEditing) return
            this.report.day = fromDateInput(this.reportDate)
            this.saveDraft()
            this.checkExistingReport()
        },
        async submitReport() {
            if (this.submitting) return
            this.errorMessage = ''
            this.today = toDateInput(Date.now())
            this.report.day = this.isEditing && this.originalDay
                ? this.originalDay
                : fromDateInput(this.reportDate)
            if (!this.report.day || (!this.isEditing && this.reportDate > this.today)) {
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
                notifyReportSubmitted({userId: saved.userId, day: saved.day})
                const from = new URLSearchParams(window.location.search).get('from')
                let fallbackUrl = 'index.html'
                if (from === 'detail') {
                    fallbackUrl = `report.html?userId=${encodeURIComponent(saved.userId)}&day=${saved.day}`
                } else if (from === 'mine') {
                    fallbackUrl = 'myReports.html'
                }
                closeEditorWindow(fallbackUrl)
            } catch (e) {
                this.errorMessage = e.message || '日报提交失败，请稍后重试'
                this.submitting = false
            }
        },
        saveDraft() {
            if (this.isEditing) return
            try {
                sessionStorage.setItem(this.draftKey, JSON.stringify({
                    report: this.report,
                    reportDate: this.reportDate,
                    userTargets: this.userTargets,
                    ccTargets: this.ccTargets
                }))
            } catch (e) {
                console.warn('Save draft failed', e)
            }
        },
        restoreDraft() {
            try {
                const draft = JSON.parse(sessionStorage.getItem(this.draftKey))
                if (draft && draft.report) {
                    Object.assign(this.report, draft.report)
                    this.report.title = this.report.title || ''
                    this.report.content = this.report.content || ''
                    this.report.tomorrowPlan = this.report.tomorrowPlan || ''
                    this.report.requirement = this.report.requirement || ''
                    this.report.reportTo = Array.isArray(this.report.reportTo) ? this.report.reportTo : []
                    this.report.ccTo = Array.isArray(this.report.ccTo) ? this.report.ccTo : []
                    this.reportDate = draft.reportDate || toDateInput(this.report.day)
                    this.userTargets = Array.isArray(draft.userTargets) ? draft.userTargets : []
                    this.ccTargets = Array.isArray(draft.ccTargets) ? draft.ccTargets : []
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
            this.checkExistingReport()
        },
        cancel() {
            const from = new URLSearchParams(window.location.search).get('from')
            if (from === 'detail' && this.originalDay) {
                closeEditorWindow(`report.html?userId=${encodeURIComponent(this.account)}&day=${this.originalDay}`)
                return
            }
            if (from === 'home' || from === 'mine') {
                closeEditorWindow(from === 'home' ? 'index.html' : 'myReports.html')
            } else {
                window.location.replace(new URL('myReports.html', window.location.href).href)
            }
        }
    }
}
</script>

<style scoped>
.editor-page {
    padding-top: 16px;
    padding-bottom: max(32px, env(safe-area-inset-bottom));
}

.editor-header {
    margin-bottom: 18px;
    text-align: left;
}

.editor-header h1 {
    color: #1f2937;
    font-size: 22px;
    font-weight: 600;
    letter-spacing: .2px;
}

.editor-form {
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.form-card {
    padding: 18px 20px;
    border: 1px solid #e4e7ec;
    border-radius: 14px;
    background: #fff;
    box-shadow: 0 2px 10px rgba(16, 24, 40, .03);
    text-align: left;
}

.meta-grid {
    display: grid;
    grid-template-columns: 1fr;
    gap: 16px;
}

@media (min-width: 560px) {
    .meta-grid {
        grid-template-columns: 188px 1fr;
        align-items: start;
    }
}

.field-group {
    display: flex;
    flex-direction: column;
    min-width: 0;
}

.field-label,
.field-heading label {
    display: block;
    color: #344054;
    font-size: 14px;
    font-weight: 600;
    line-height: 1.4;
}

.field-group input {
    display: block;
    width: 100%;
    margin-top: 8px;
    padding: 10px 14px;
    border: 1px solid #d0d5dd;
    border-radius: 10px;
    background: #f9fafb;
    color: #1f2937;
    font-size: 14px;
    line-height: 1.5;
}

.field-group input:hover:not(:disabled) {
    border-color: #b8c2d0;
    background: #fff;
}

.field-group input:focus {
    border-color: #2f6fed;
    background: #fff;
    box-shadow: 0 0 0 3px rgba(47, 111, 237, .12);
}

.field-group input:disabled {
    border-color: #e4e7ec;
    background: #f2f4f7;
    color: #667085;
    cursor: not-allowed;
}

.field-hint {
    margin-top: 6px;
    color: #667085;
    font-size: 12px;
}

.field-heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
}

.char-count {
    padding: 2px 8px;
    border-radius: 999px;
    background: #f2f4f7;
    color: #667085;
    font-size: 12px;
    font-variant-numeric: tabular-nums;
}

em {
    color: #d92d20;
    font-style: normal;
}

.editor-textarea {
    display: block;
    width: 100%;
    min-height: 104px;
    margin-top: 10px;
    padding: 12px 14px;
    border: 1px solid #e4e7ec;
    border-radius: 10px;
    background: #f9fafb;
    color: #1f2937;
    font: inherit;
    font-size: 15px;
    line-height: 1.75;
    resize: vertical;
    transition: border-color .16s ease, background-color .16s ease, box-shadow .16s ease;
}

.editor-textarea-lg {
    min-height: 144px;
}

.editor-textarea:hover {
    border-color: #d0d5dd;
    background: #fff;
}

.editor-textarea:focus {
    border-color: #2f6fed;
    background: #fff;
    box-shadow: 0 0 0 3px rgba(47, 111, 237, .12);
}

.editor-textarea::placeholder {
    color: #98a2b3;
}

.recipients-card {
    padding: 4px 0;
    overflow: hidden;
}

.recipient-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    width: 100%;
    min-height: 58px;
    padding: 0 20px;
    border: 0;
    border-bottom: 1px solid #edf0f4;
    background: transparent;
    color: #344054;
    font-size: 14px;
    text-align: left;
    cursor: pointer;
    transition: background-color .14s ease;
}

.recipient-row:hover {
    background: #f9fafb;
}

.recipient-row:active {
    background: #f2f4f7;
}

.recipient-row:last-child {
    border-bottom: 0;
}

.recipient-label {
    flex: 0 0 auto;
    font-weight: 600;
}

.recipient-row strong {
    display: inline-flex;
    align-items: center;
    max-width: 68%;
    overflow: hidden;
    color: #1f2937;
    font-size: 14px;
    font-weight: 500;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.recipient-row strong.is-placeholder {
    color: #98a2b3;
    font-weight: 400;
}

.recipient-row i {
    margin-left: 8px;
    color: #98a2b3;
    font-size: 20px;
    font-style: normal;
    line-height: 1;
}

.submit-section {
    margin-top: 4px;
}

.submit-button {
    width: 100%;
    min-height: 48px;
    padding: 13px 20px;
    border-radius: 12px;
    font-size: 15px;
    font-weight: 600;
    box-shadow: 0 6px 16px rgba(47, 111, 237, .2);
}

.submit-note {
    margin: 12px 0 8px;
    color: #667085;
    font-size: 12px;
    line-height: 1.6;
    text-align: center;
}

.draft-tip {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 10px 14px;
    border: 1px solid #fedf89;
    border-radius: 10px;
    background: #fffaeb;
    color: #93370d;
    font-size: 13px;
    line-height: 1.5;
    text-align: left;
}

.draft-tip button {
    flex: 0 0 auto;
    min-height: 36px;
    padding: 4px 10px;
    border: 1px solid #fec84b;
    border-radius: 8px;
    background: #fff;
    color: #93370d;
    font-size: 12px;
    font-weight: 500;
    cursor: pointer;
}

.draft-tip button:hover {
    background: #fef0c7;
}

.error-state strong {
    color: #1f2937;
    font-size: 17px;
}

.error-state p {
    margin: 10px 0 20px;
    color: #667085;
    font-size: 14px;
}

.error-actions {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 10px;
}

@media (max-width: 480px) {
    .form-card {
        padding: 15px 16px;
    }

    .recipient-row {
        padding: 0 16px;
    }

    .editor-textarea {
        padding: 10px 12px;
    }
}
</style>
