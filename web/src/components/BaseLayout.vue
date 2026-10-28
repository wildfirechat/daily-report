<template>
    <div id="base-container">
        <SplashView v-if="status === 0"/>
        <slot v-else-if="status === 1" :account="account"/>
        <ErrorView v-else @retry="retryLogin"/>
    </div>
</template>

<script>
import SplashView from "@/components/SplashView";
import ErrorView from "@/components/ErrorView";
import api from "@/api/api";
import wf from "@/jssdk/wf";

export default {
    name: 'BaseLayout',
    components: {SplashView, ErrorView},
    data() {
        return {
            account: '',
            loginStatus: 0,
            configStatus: 0
        }
    },
    created() {
        this.getAccount();
        wf.ready(() => {
            this.configStatus = 1;
        })
        wf.error(() => {
            this.configStatus = -1;
        })
    },
    methods: {
        async getAccount() {
            api.getAccount().then(account => {
                this.account = account;
                this.loginStatus = 1;
            }).catch(() => {
                this.login()
            })
            try {
                const configData = await api.getConfigData();
                wf.config(configData);
            } catch (e) {
                this.loginStatus = -1;
            }
        },
        async login() {
            let configData;
            try {
                configData = await api.getConfigData();
                wf.config(configData);
            } catch (e) {
                this.loginStatus = -1;
                return;
            }
            wf.biz.getAuthCode(configData.appId, configData.appType, (authCode) => {
                api.login({
                    appId: configData.appId,
                    appType: configData.appType,
                    authCode: authCode
                }).then((account) => {
                    this.loginStatus = 1;
                    this.account = account;
                }).catch(() => {
                    this.loginStatus = -1;
                })
            }, () => {
                this.loginStatus = -1;
            })
        },
        retryLogin() {
            this.loginStatus = 0;
            this.configStatus = 0;
            this.getAccount();
        }
    },
    computed: {
        status() {
            if (this.loginStatus === 1 && this.configStatus === 1) {
                return 1;
            } else if (this.loginStatus === -1 || this.configStatus === -1) {
                return -1;
            }
            return 0;
        }
    }
}
</script>

<style lang="css" scoped>
@import "../assets/main.css";

#base-container {
    min-height: 100vh;
    text-align: center;
}
</style>
