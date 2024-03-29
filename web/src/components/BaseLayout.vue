<template>
    <div id="base-container">
        <SplashView v-if="status === 0"/>
        <slot v-else-if="status === 1" :account="account"/>
        <ErrorView v-else/>
    </div>
</template>

<script>
import SplashView from "@/components/SplashView";
import ErrorView from "@/components/ErrorView";
import api from "@/api/api";
import wf from "@/jssdk/wf";

export default {
    name: 'BaseLayout',
    components: {
        SplashView,
        ErrorView,
    },
    data() {
        return {
            account: '',
            loginStatus: 0, //0,登录中；1，登录成功；-1 登录失败
            configStatus: 0, //native 是否成功认证 web 0，认证中；1，认证成功；-1，认证失败。只有 native 成功认证 web 和登录成功只有，才能开始正式的业务逻辑
        }
    },

    created() {
        this.getAccount();
        wf.ready(() => {
            // native 认证 web 成功，可以开始调用 wf.js 里面的 api
            console.log('config ready');
            this.configStatus = 1;
        })
        wf.error(() => {
            // native 认证 web 失败
            console.log('config error');
            this.configStatus = -1;
        })
    },
    methods: {
        async getAccount() {
            api.getAccount().then(account => {
                this.account = account;
                this.loginStatus = 1;
            }).catch(r => {
                console.log('getAccount error, to login', r)
                this.login()
            })
            api.getConfigData().then(configData => {
                // native 认证 web
                wf.config(configData);
            });
        },
        async login() {
            let configData;
            try {
                configData = await api.getConfigData();
                // native 认证 web
                wf.config(configData);
                console.log('configData', configData);
            } catch (e) {
                console.log('getConfigData failed', e)
                this.loginStatus = -1;
                return;
            }
            // web 从 native 获取授权码，并进行登录
            wf.biz.getAuthCode(configData.appId, configData.appType, (authCode) => {
                api.login({
                    appId: configData.appId,
                    appType: configData.appType,
                    authCode: authCode
                }).then((account) => {
                    this.loginStatus = 1;
                    this.account = account;
                    console.log('login success')
                }).catch(reason => {
                    this.loginStatus = -1;
                    console.log('login failed', reason)
                })
            }, (err) => {
                console.error('getAuthCode error', err)
                this.loginStatus = -1;
            })
        },
    },

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
@import "../assets/main.css";

#base-container {
    font-family: Avenir, Helvetica, Arial, sans-serif;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
    text-align: center;
    color: #2c3e50;
    height: 100%;
}
</style>
