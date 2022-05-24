<template>
    <BaseLayout ref="base">
        <ReportListView/>
    </BaseLayout>
</template>

<script>
import ReportListView from "@/components/ReportListView";
import BaseLayout from "@/components/BaseLayout";

export default {
    name: 'ReportListPage',
    components: {
        ReportListView,
        BaseLayout,
    },
    data() {
        return {}
    },

    created() {
        document.title = '日志列表';
    },
    methods: {},

    computed: {}
}
</script>

<style lang="css" scoped>
</style>
