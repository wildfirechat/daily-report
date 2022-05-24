<template>
    <BaseLayout>
        <ReportView :report="report"/>
    </BaseLayout>
</template>

<script>
import ReportView from "@/components/ReportView";
import BaseLayout from "@/components/BaseLayout";
import api from "@/api/api";

export default {
    name: 'ReportPage',
    components: {
        ReportView,
        BaseLayout,
    },
    data() {
        return {
            report: null,
        }
    },

    created() {
        document.title = '日志详情';
        let day = this._getQuery(location.href, 'day')
        api.getReport(day).then(report => {
            this.report = report;
        })
    },
    methods: {
        _getQuery(url, key) {
            let query = url.split('?');
            if (query && query.length > 1) {
                let params = new URLSearchParams(query[1]);
                return params.get(key);
            }
            return null;
        },
    },

    computed: {}
}
</script>

<style lang="css" scoped>
</style>
