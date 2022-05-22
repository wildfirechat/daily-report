<template>
    <div id="app">
        <SplashView v-if="status === 0"/>
        <WorkReport v-else-if="status === 1" :account="account"/>
        <ErrorPage v-else/>
    </div>
</template>

<script>
import WorkReport from './components/WorkReportPage.vue'
import SplashView from "@/components/SplashPage";
import ErrorPage from "@/components/ErrorPage";
import api from "@/api/api";
import wf from "@/jssdk/wf";

export default {
    name: 'App',
    components: {
        WorkReport,
        SplashView,
        ErrorPage,
    },
    data() {
        return {
            account: '',
            loginStatus: 0, //0,登录中；1，登录成功；-1 登录失败
            configStatus: 0, //native 是否成功认证 web 0，认证中；1，认证成功；-1，认证失败。只有 native 成功认证 web 和登录成功只有，才能开始正式的业务逻辑
        }
    },

    created() {
        document.title = '工作日志';
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
@import "./assets/main.css";

#app {
    font-family: Avenir, Helvetica, Arial, sans-serif;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
    text-align: center;
    color: #2c3e50;
    padding-top: 10px;
    height: 100%;
}
</style>
