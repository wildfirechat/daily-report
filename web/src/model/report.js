export default class Report {
    userId;
    title = '日报';
    content = '';
    tomorrowPlan = '';
    requirement = '';
    reportTo = [];
    day = Date.now();
    ccTo = [];
    reportToGroup = [];
    ccToGroup = [];
    displayName = '';
    createdAt = null;
    updatedAt = null;

    constructor(userId) {
        this.userId = userId;
    }

    valid() {
        if (!this.content || !this.content.trim()) {
            return {code: -1, msg: '今日工作总结不能为空'}
        }
        if (this.title && this.title.length > 64) {
            return {code: -1, msg: '标题不能超过 64 个字符'}
        }
        if (!this.reportTo || this.reportTo.length === 0) {
            return {code: -1, msg: '请选择汇报对象'}
        }
        return {code: 0}
    }
}
