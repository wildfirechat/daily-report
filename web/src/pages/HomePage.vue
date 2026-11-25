<template>
    <BaseLayout v-slot="{account}">
        <AppShell :account="account" initial-view="home"/>
    </BaseLayout>
</template>

<script>
import '../jssdk/bridgeClientImpl.uni'
import BaseLayout from "@/components/BaseLayout";
import AppShell from "@/components/AppShell";

export default {
    name: 'HomePage',
    components: {BaseLayout, AppShell},
    created() {
        document.title = '工作日报'
    }
}
</script>
