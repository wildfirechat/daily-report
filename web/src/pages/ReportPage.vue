<template>
    <BaseLayout v-slot="{account}">
        <ReportView :account="account"/>
    </BaseLayout>
</template>

<script>
import '../jssdk/bridgeClientImpl.uni'
import ReportView from "@/components/ReportView";
import BaseLayout from "@/components/BaseLayout";

export default {
    name: 'ReportPage',
    components: {BaseLayout, ReportView},
    created() {
        document.title = '日报详情';
    }
}
</script>
