<template>
    <BaseLayout v-slot="{account}">
        <ReportListView :account="account" mode="mine"/>
    </BaseLayout>
</template>

<script>
import '../jssdk/bridgeClientImpl.uni'
import ReportListView from "@/components/ReportListView";
import BaseLayout from "@/components/BaseLayout";

export default {
    name: 'ReportListPage',
    components: {ReportListView, BaseLayout},
    created() {
        document.title = '我的日报';
    }
}
</script>
