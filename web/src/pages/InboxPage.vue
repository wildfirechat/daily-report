<template>
    <BaseLayout v-slot="{account}">
        <ReportListView :account="account" mode="received"/>
    </BaseLayout>
</template>

<script>
import '../jssdk/bridgeClientImpl.uni'
import ReportListView from "@/components/ReportListView";
import BaseLayout from "@/components/BaseLayout";

export default {
    name: 'InboxPage',
    components: {ReportListView, BaseLayout},
    created() {
        document.title = '汇报箱';
    }
}
</script>
