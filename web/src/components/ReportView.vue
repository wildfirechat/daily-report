<template>
    <div>
        <p v-if="!report"></p>
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
export default {
    name: "ReportView",
    props: {
        report: {
            type: Object,
            required: false,
        }
    },
    created() {
        console.log('ReportView', location.href)
    },
    methods: {
        reportTitle(report) {
            let day = new Date(report.day);
            return day.toLocaleDateString();
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