<template>
    <div class="report-list-container">
        <div class="action-container">
            <button @click="writeReport">
                写日志
            </button>
        </div>
        <ul>
            <li v-for="(report, index) in reports" :key="index">
                <div class="report-container" @click="showReportDetail(report)">
                    <p>{{ reportTitle(report) }}</p>
                    <div class="report-item">
                        <p class="title">今日完成工作：</p>
                        <p class="content">{{ report.content }}</p>
                    </div>
                    <div class="report-item">
                        <p class="title">明日计划：</p>
                        <p class="content">{{ report.tomorrowPlan }}</p>
                    </div>
                    <div class="report-item">
                        <p class="title">需协调工作：</p>
                        <p class="content">{{ report.requirement }}</p>
                    </div>
                </div>
            </li>
        </ul>
    </div>
</template>

<script>
import wf from "@/jssdk/wf";
import api from "@/api/api";

export default {
    name: "ReportListView",
    data() {
        return {
            reports: [],
        }
    },
    created() {
        api.getReportList(20, 0).then(reports => {
            this.reports.push(...reports);
        })
    },
    methods: {
        writeReport() {
            wf.openUrl(location.href + 'newReport', {external: true})
        },
        showReportDetail(report) {
            wf.openUrl(location.href + 'report' + '?day=' + report.day, {external: true})
        },
        reportTitle(report) {
            let day = new Date(report.day);
            return day.toLocaleDateString();
        },
    },
}
</script>

<style scoped>

/*.report-list-container {*/
/*    padding: 10px 20px;*/
/*}*/

.action-container {
    padding: 20px;
    text-align: left;
}

.action-container button {
    padding: 5px 10px;
}

.report-container {
    padding: 10px 20px;
    position: relative;
}

.report-container:after {
    position: absolute;
    content: '';
    border-bottom: 1px solid #d2d7da;
    width: 100%;
    transform: translateX(-50%);
    bottom: 0;
    margin: 0 20px;
}

.report-container:active {
    background-color: lightgrey;
}

.report-item {
    display: flex;
    flex-direction: row;
}

.report-item .title {
    font-weight: bold;
    width: 120px;
    text-align: left;
}

.report-item .content {
    flex: 1;
    text-align: left;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

</style>