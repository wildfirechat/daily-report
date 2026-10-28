<template>
    <BaseLayout>
        <HomeView/>
    </BaseLayout>
</template>

<script>
import BaseLayout from "@/components/BaseLayout";
import HomeView from "@/components/HomeView";

export default {
    name: 'HomePage',
    components: {BaseLayout, HomeView},
    created() {
        document.title = '工作日报'
    }
}
</script>
