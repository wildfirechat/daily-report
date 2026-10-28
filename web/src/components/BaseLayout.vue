<template>
    <div id="base-container">
        <SplashView v-if="status === 0"/>
        <slot v-else-if="status === 1" :account="account"/>
        <ErrorView v-else :type="loginErrorType" @retry="retryLogin"/>
        <ToastHost/>
    </div>
</template>

<script>
import SplashView from "@/components/SplashView";
import ErrorView from "@/components/ErrorView";
import ToastHost from "@/components/ToastHost";
import api from "@/api/api";
import wf from "@/jssdk/wf";
import {isBridgeAvailable} from "@/jssdk/util";

const LOGIN_TIMEOUT_MS = 15000

export default {
    name: 'BaseLayout',
    components: {SplashView, ErrorView, ToastHost},
    data() {
        return {
            account: '',
            loginStatus: 0,
            configStatus: 0,
            bridgeReady: false,
            loginErrorType: 'unknown',
            loginTimer: null,
            loginAttempt: 0,
            pendingAuth: null
        }
    },
    created() {
        // 桌面端桥接由客户端注入，注入失败时直接给出明确错误，避免抛异常卡在启动页。
        if (!isBridgeAvailable()) {
            this.failLogin('bridge')
            return
        }
        wf.ready(() => {
            this.bridgeReady = true
            this.configStatus = 1
            this.finishLogin()
            this.runPendingAuth()
        })
        wf.error(() => {
            this.bridgeReady = false
            this.failLogin('bridge', this.loginAttempt)
        })
        this.beginLogin()
    },
    beforeUnmount() {
        this.clearLoginTimeout()
    },
    methods: {
        beginLogin() {
            this.clearLoginTimeout()
            const attempt = ++this.loginAttempt
            this.pendingAuth = null
            this.account = ''
            this.loginStatus = 0
            this.configStatus = this.bridgeReady ? 1 : 0
            this.loginErrorType = 'unknown'
            this.startLoginTimeout(attempt)
            this.getAccount(attempt)
        },
        async getAccount(attempt) {
            try {
                const account = await api.getAccount()
                if (attempt !== this.loginAttempt) return
                this.account = account
                this.loginStatus = 1
                this.finishLogin()
                await this.configureBridge(attempt)
            } catch (e) {
                if (attempt === this.loginAttempt) {
                    await this.login(attempt)
                }
            }
        },
        async configureBridge(attempt) {
            let configData
            try {
                configData = await api.getConfigData()
            } catch (e) {
                this.failLogin('network', attempt)
                return
            }
            if (attempt !== this.loginAttempt) return
            try {
                wf.config(configData)
            } catch (e) {
                this.failLogin('bridge', attempt)
            }
        },
        async login(attempt) {
            if (attempt !== this.loginAttempt) return
            let configData
            try {
                configData = await api.getConfigData()
                if (attempt !== this.loginAttempt) return
            } catch (e) {
                this.failLogin('network', attempt)
                return
            }

            // 客户端 config 是异步操作，getAuthCode 必须在 ready 之后调用；
            // 这里先挂起取码逻辑，等 ready 事件（或已配置成功）后再执行。
            this.pendingAuth = () => {
                if (attempt !== this.loginAttempt) return
                try {
                    wf.biz.getAuthCode(configData.appId, configData.appType, (authCode) => {
                        if (attempt !== this.loginAttempt) return
                        api.login({
                            appId: configData.appId,
                            appType: configData.appType,
                            authCode: authCode
                        }).then((account) => {
                            if (attempt !== this.loginAttempt) return
                            this.account = account
                            this.loginStatus = 1
                            this.finishLogin()
                        }).catch(() => {
                            this.failLogin('auth', attempt)
                        })
                    }, () => {
                        this.failLogin('bridge', attempt)
                    })
                } catch (e) {
                    this.failLogin('bridge', attempt)
                }
            }

            if (this.bridgeReady) {
                this.runPendingAuth()
                return
            }
            try {
                wf.config(configData)
            } catch (e) {
                this.pendingAuth = null
                this.failLogin('bridge', attempt)
            }
        },
        runPendingAuth() {
            const pending = this.pendingAuth
            this.pendingAuth = null
            if (pending) pending()
        },
        startLoginTimeout(attempt) {
            this.loginTimer = window.setTimeout(() => {
                if (attempt === this.loginAttempt && this.status === 0) {
                    this.failLogin('timeout', attempt)
                }
            }, LOGIN_TIMEOUT_MS)
        },
        clearLoginTimeout() {
            if (this.loginTimer) {
                window.clearTimeout(this.loginTimer)
                this.loginTimer = null
            }
        },
        finishLogin() {
            if (this.loginStatus === 1 && this.configStatus === 1) {
                this.clearLoginTimeout()
                this.loginErrorType = 'unknown'
            }
        },
        failLogin(type, attempt) {
            if (attempt !== undefined && attempt !== this.loginAttempt) {
                return
            }
            if (this.loginStatus === 1 && this.configStatus === 1) {
                return
            }
            this.loginErrorType = type
            this.loginStatus = -1
            this.clearLoginTimeout()
        },
        retryLogin() {
            this.beginLogin()
        }
    },
    computed: {
        status() {
            if (this.loginStatus === 1 && this.configStatus === 1) {
                return 1
            } else if (this.loginStatus === -1 || this.configStatus === -1) {
                return -1
            }
            return 0
        }
    }
}
</script>

<style lang="css" scoped>
#base-container {
    min-height: 100vh;
    text-align: center;
}
</style>
