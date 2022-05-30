<template>
    <div>
        <p v-if="!report">日报不存在</p>
        <div v-else class="report-container">
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
    </div>
</template>

<script>
import api from "@/api/api";

export default {
    name: "ReportView",
    data() {
        return {
            report: {},
        }
    },
    created() {
        console.log('ReportView', location.href)
        this.getReport();
    },
    methods: {
        reportTitle(report) {
            let day = new Date(report.day);
            return day.toLocaleDateString();
        },

        getReport() {
            let day = this._getQuery(location.href, 'day')
            api.getReport(day).then(report => {
                this.report = report;
            })
        },

        _getQuery(url, key) {
            let query = url.split('?');
            if (query && query.length > 1) {
                let params = new URLSearchParams(query[1]);
                return params.get(key);
            }
            return null;
        },
    }
}
</script>

<style scoped>

.report-container {
    padding: 10px 20px;
    position: relative;
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