<template>
    <BaseLayout v-slot="{account}">
        <AppShell :account="account" initial-view="mine"/>
    </BaseLayout>
</template>

<script>
import '../jssdk/bridgeClientImpl.uni'
import AppShell from "@/components/AppShell";
import BaseLayout from "@/components/BaseLayout";

export default {
    name: 'ReportListPage',
    components: {AppShell, BaseLayout},
    created() {
        document.title = '我的日报';
    }
}
</script>
