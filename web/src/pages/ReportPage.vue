<template>
    <BaseLayout v-slot="{account}">
        <AppShell :account="account" initial-view="detail"/>
    </BaseLayout>
</template>

<script>
import '../jssdk/bridgeClientImpl.uni'
import AppShell from "@/components/AppShell";
import BaseLayout from "@/components/BaseLayout";

export default {
    name: 'ReportPage',
    components: {BaseLayout, AppShell},
    created() {
        document.title = '日报详情';
    }
}
</script>
