<template>
    <BaseLayout v-slot="{account}">
        <NewReportView :account="account"/>
    </BaseLayout>
</template>

<script>
import '../jssdk/bridgeClientImpl.uni'
import NewReportView from "@/components/NewReportView";
import BaseLayout from "@/components/BaseLayout";

export default {
    name: 'NewReportPage',
    components: {BaseLayout, NewReportView},
    created() {
        document.title = '写日报';
    }
}
</script>
