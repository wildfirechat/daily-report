/**
 * 页面启动自检：
 * 1. 静态资源（js/css）加载失败时把失败地址显示出来，避免 Electron webview 里只看到白屏；
 * 2. Vue 在 8 秒内没有挂载时提示可能的原因。
 * 只接管 #app 里还没有 Vue 内容的情况，已挂载后不覆盖页面。
 */
(function () {
    var reported = false

    function isEmptyApp(app) {
        if (!app) {
            return false
        }
        return app.hasAttribute('data-v-app') === false && !!app.querySelector('.boot-fallback')
    }

    function showError(message) {
        var app = document.getElementById('app')
        if (!app || !isEmptyApp(app)) {
            // 已经挂载，仅记录日志，不破坏页面
            console.error('[daily-report] boot error:', message)
            return
        }
        if (reported) {
            return
        }
        reported = true

        var wrap = document.createElement('div')
        wrap.className = 'boot-error'

        var title = document.createElement('h2')
        title.textContent = '页面加载失败'
        wrap.appendChild(title)

        var desc = document.createElement('p')
        desc.textContent = message + '。如刚更新过服务，请关闭标签页重新打开或清理客户端缓存后重试。'
        wrap.appendChild(desc)

        var button = document.createElement('button')
        button.type = 'button'
        button.textContent = '重新加载'
        button.addEventListener('click', function () {
            window.location.reload()
        })
        wrap.appendChild(button)

        app.innerHTML = ''
        app.appendChild(wrap)
    }

    window.__reportBootError = showError

    window.addEventListener('error', function (event) {
        if (event && event.target && event.target.tagName === 'SCRIPT') {
            showError('脚本加载失败：' + (event.target.src || '未知地址'))
        } else if (event && event.target && event.target.tagName === 'LINK') {
            showError('样式加载失败：' + (event.target.href || '未知地址'))
        } else if (event && event.message) {
            showError(event.message)
        }
    }, true)

    window.addEventListener('unhandledrejection', function (event) {
        var reason = event && event.reason
        showError((reason && reason.message) || String(reason || '未知错误'))
    })

    window.setTimeout(function () {
        var app = document.getElementById('app')
        if (isEmptyApp(app)) {
            showError('页面脚本未能在 8 秒内启动')
        }
    }, 8000)
})()
