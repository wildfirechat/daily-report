import {createApp} from 'vue'

const BUILD_TIME = typeof __BUILD_TIME__ !== 'undefined' ? __BUILD_TIME__ : 'dev'

/**
 * 统一创建并挂载 Vue 应用：
 * - 渲染/生命周期异常时输出日志，并交给 boot-check.js 在“尚未挂载成功”时展示错误；
 * - 打印构建时间，便于确认客户端实际加载的是哪一版页面。
 */
export function mountApp(App) {
    console.log('[daily-report] build', BUILD_TIME)

    const app = createApp(App)
    app.config.errorHandler = (err, instance, info) => {
        console.error('[daily-report] vue error', info, err)
        if (typeof window.__reportBootError === 'function') {
            window.__reportBootError((err && err.message) || '页面渲染失败')
        }
    }
    app.mount('#app')
    return app
}
