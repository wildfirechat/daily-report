import Axios from 'axios'

// Keep the bearer-style token in memory. The HttpOnly session cookie remains the primary
// credential for same-origin browser navigation; persisting the token in localStorage makes it
// readable by any XSS or third-party script.
let authToken = null
let reloginHandler = null
let reloginPromise = null

const SKIP_RELOGIN_URLS = new Set(['/account', '/user/login', '/user/config', '/login', '/logout'])

export function setReloginHandler(handler) {
    reloginHandler = typeof handler === 'function' ? handler : null
}

function shouldAttemptRelogin(config) {
    if (!config || config._retried || !reloginHandler) {
        return false
    }
    return !SKIP_RELOGIN_URLS.has(config.url)
}

function attemptReloginAndRetry(config) {
    authToken = null
    if (!reloginPromise) {
        reloginPromise = Promise.resolve()
            .then(() => reloginHandler())
            .finally(() => {
                reloginPromise = null
            })
    }
    return reloginPromise.then(
        () => {
            config._retried = true
            if (config.headers) {
                delete config.headers['authToken']
                delete config.headers['X-XSRF-TOKEN']
            }
            return instance(config)
        },
        () => Promise.reject({
            code: 13,
            message: '登录状态已过期，请刷新页面重新登录'
        })
    )
}

function readCookie(name) {
    const prefix = name + '='
    const parts = document.cookie ? document.cookie.split('; ') : []
    for (const part of parts) {
        if (part.startsWith(prefix)) {
            return decodeURIComponent(part.substring(prefix.length))
        }
    }
    return null
}

const instance = Axios.create({
    baseURL: '/api',
    withCredentials: true,
    headers: {
        'Content-Type': 'application/json;charset=utf-8',
        'Accept': 'application/json;charset=utf-8'
    }
})

instance.interceptors.request.use(request => {
    if (authToken) {
        request.headers['authToken'] = authToken
    }
    const csrfToken = readCookie('XSRF-TOKEN')
    if (csrfToken) {
        request.headers['X-XSRF-TOKEN'] = csrfToken
    }
    return request
})

instance.interceptors.response.use(response => {
    const {code, message, result} = response.data
    if (code === 0) {
        if (response.config.url === '/user/login') {
            authToken = response.headers['authtoken'] || response.headers['authToken'] || null
        }
        return result
    }
    if (code === 13 && shouldAttemptRelogin(response.config)) {
        return attemptReloginAndRetry(response.config)
    }
    if (code === 13) {
        authToken = null
    }
    return Promise.reject({code, message})
}, error => {
    const data = error.response && error.response.data
    const isNotLogin = (data && data.code === 13) || (error.response && error.response.status === 401)
    if (isNotLogin) {
        authToken = null
        if (shouldAttemptRelogin(error.config)) {
            return attemptReloginAndRetry(error.config)
        }
    }
    return Promise.reject({
        code: data && data.code,
        message: (data && data.message) || '网络连接失败，请检查网络后重试'
    })
})

export default instance
