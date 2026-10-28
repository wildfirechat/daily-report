import wf from '@/jssdk/wf'

export function openPage(path) {
    const url = new URL(path, window.location.href).href
    const userAgent = navigator.userAgent.toLowerCase()
    if (userAgent.includes('wf-dsbridge') || userAgent.includes('uni-app') || userAgent.includes('electron')) {
        wf.openUrl(url, {external: true})
    } else {
        window.location.assign(url)
    }
}
