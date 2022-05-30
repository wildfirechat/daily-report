<template>
    <BaseLayout>
        <ReportView/>
    </BaseLayout>
</template>

<script>
import ReportView from "@/components/ReportView";
import BaseLayout from "@/components/BaseLayout";

export default {
    name: 'ReportPage',
    components: {
        ReportView,
        BaseLayout,
    },

    created() {
        document.title = '日志详情';
    },

    methods: {},

    computed: {}
}
</script>

<style lang="css" scoped>
</style>
