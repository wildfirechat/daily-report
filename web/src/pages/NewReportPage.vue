<template>
    <BaseLayout>
        <NewReportView account="todo"/>
    </BaseLayout>
</template>

<script>
import '../jssdk/bridgeClientImpl.uni'
import NewReportView from "@/components/NewReportView";
import BaseLayout from "@/components/BaseLayout";

export default {
    name: 'NewReportPage',
    components: {
        BaseLayout,
        NewReportView,
    },
    data() {
        return {}
    },

    created() {
        document.title = '写日志';
    },
    methods: {},

    computed: {
        status() {
            if (this.loginStatus === 1 && this.configStatus === 1) {
                return 1;
            } else if (this.loginStatus === -1 || this.configStatus === -1) {
                return -1;
            } else {
                return 0;
            }
        }
    }
}
</script>

<style lang="css" scoped>

</style>
