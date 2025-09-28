import Vue from 'vue'
import App from './pages/ReportListPage'
// import VConsole from 'vconsole';

Vue.config.productionTip = false

// 开发调试时，可打开下面的 vConsole，方便查看日志
// const vConsole = new VConsole();

new Vue({
  render: h => h(App),
}).$mount('#app')
