<template>
    <BaseLayout v-slot="{account}">
        <AppShell :account="account" initial-view="received"/>
    </BaseLayout>
</template>

<script>
import '../jssdk/bridgeClientImpl.uni'
import AppShell from "@/components/AppShell";
import BaseLayout from "@/components/BaseLayout";

export default {
    name: 'InboxPage',
    components: {AppShell, BaseLayout},
    created() {
        document.title = '汇报箱';
    }
}
</script>
