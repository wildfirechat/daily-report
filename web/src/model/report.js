export default class Report {
    userId;
    title;
    content;
    tomorrowPlan;
    requirement;
    reportTo = [];
    day = 1653314400000;
    ccTo = [];
    reportToGroup = [];
    ccToGroup = [];


    constructor(userId) {
        this.userId = userId;
    }

    valid() {
        if (!this.content || !this.content.trim()) {
            return {
                code: -1,
                msg: '汇报内容不能为空'
            }
        }

        if (!this.reportTo || this.reportTo.length === 0) {
            return {}
        }
    }
}