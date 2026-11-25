import wf from '@/jssdk/wf'

export const SUBMITTED_KEY = 'daily-report:submitted'
const SUBMITTED_TTL_MS = 15000

export function isNativeContainer() {
    const userAgent = navigator.userAgent.toLowerCase()
    return userAgent.includes('wf-dsbridge')
        || userAgent.includes('uni-app')
        || userAgent.includes('electron')
        || (typeof process !== 'undefined' && process.versions && !!process.versions.electron)
}

function safeNativeClose() {
    try {
        wf.navigation.close()
        return true
    } catch (e) {
        console.warn('Close page failed', e)
        return false
    }
}

function openUrlOptions(options) {
    return {external: Boolean(options && options.external)}
}

export function openPage(path, options) {
    const url = new URL(path, window.location.href).href
    if (isNativeContainer()) {
        wf.openUrl(url, openUrlOptions(options))
    } else {
        window.location.assign(url)
    }
}

export function openEditorWindow(path) {
    const url = new URL(path, window.location.href).href
    if (isNativeContainer()) {
        wf.openUrl(url, {external: true})
        return
    }
    const popup = window.open(url, '_blank')
    if (!popup) {
        window.location.assign(url)
    }
}

export function closeEditorWindow(fallbackPath) {
    const url = new URL(fallbackPath, window.location.href).href
    if (isNativeContainer()) {
        if (!safeNativeClose()) {
            window.location.replace(url)
        }
        return
    }
    if (window.opener && !window.opener.closed) {
        window.close()
        return
    }
    window.location.replace(url)
}

export function replacePage(path, options) {
    const url = new URL(path, window.location.href).href
    if (isNativeContainer()) {
        wf.openUrl(url, openUrlOptions(options))
        safeNativeClose()
    } else {
        window.location.replace(url)
    }
}

export function closeOrNavigate(fallbackPath, options) {
    const url = new URL(fallbackPath, window.location.href).href
    if (isNativeContainer()) {
        if (!safeNativeClose()) {
            wf.openUrl(url, openUrlOptions(options))
        }
    } else {
        window.location.assign(url)
    }
}

export function closeOrReplace(fallbackPath, options) {
    const url = new URL(fallbackPath, window.location.href).href
    if (isNativeContainer()) {
        if (!safeNativeClose()) {
            wf.openUrl(url, openUrlOptions(options))
        }
    } else {
        window.location.replace(url)
    }
}

export function notifyReportSubmitted({userId, day}) {
    try {
        localStorage.setItem(SUBMITTED_KEY, JSON.stringify({
            userId,
            day,
            expiresAt: Date.now() + SUBMITTED_TTL_MS
        }))
    } catch (e) {
        // Storage may be unavailable in restricted WebViews.
    }
}

export function consumeReportSubmitted() {
    try {
        const raw = localStorage.getItem(SUBMITTED_KEY)
        if (!raw) {
            return null
        }
        localStorage.removeItem(SUBMITTED_KEY)
        const parsed = JSON.parse(raw)
        if (parsed && parsed.expiresAt && Date.now() > parsed.expiresAt) {
            return null
        }
        return parsed
    } catch (e) {
        return null
    }
}
