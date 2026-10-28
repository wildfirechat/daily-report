/**
 * 开放平台 JS-SDK Client (Web 版本)
 *
 * 运行在 iframe 中，通过 postMessage 与父窗口通信
 * 协议格式与 Electron WebSocket 版本保持一致
 */

let callbackMap = new Map();
let eventListeners = {};
let requestId = 0;
let initialized = false;

const windowId = 'web-tab';

const configuredParentOrigins = (window.__WF_ALLOWED_PARENT_ORIGINS__
    || (document.querySelector('meta[name="wf-allowed-parent-origins"]') || {}).content
    || '')
    .split(',')
    .map(origin => origin.trim())
    .filter(Boolean);

function getParentOrigin() {
    try {
        if (window.location.ancestorOrigins && window.location.ancestorOrigins.length) {
            return window.location.ancestorOrigins[0];
        }
        if (document.referrer) {
            return new URL(document.referrer).origin;
        }
    } catch (e) {
        // Native/WebView environments may not expose an origin.
    }
    return null;
}

const parentOrigin = getParentOrigin();

function isAllowedParentOrigin(origin) {
    if (configuredParentOrigins.length) {
        return configuredParentOrigins.includes(origin);
    }
    // If the embedding origin is known, only accept messages from that exact origin.
    // For native WebViews without an origin, event.source === window.parent is the only check.
    return !parentOrigin || origin === parentOrigin;
}

export function initWeb() {
    if (initialized) {
        return;
    }

    window.addEventListener('message', (event) => {
        // 只接受来自父窗口、且来源 origin 受信任的消息
        if (event.source !== window.parent || !isAllowedParentOrigin(event.origin)) {
            return;
        }

        const obj = event.data;
        if (!obj || !obj.type) {
            return;
        }

        // 过滤：检查 windowId 和 appUrl
        if (typeof obj.windowId !== 'string' || !obj.windowId.startsWith(windowId)
            || obj.appUrl !== location.href) {
            console.log('ignore wf-op data', obj);
            return;
        }

        if (obj.type === 'wf-op-event') {
            handleOpEvent(obj.handlerName, obj.args);
        } else if (obj.type === 'wf-op-response') {
            handleOpResponse(obj.requestId, obj.args);
        }
    });

    initialized = true;

    window.__wf_bridge_ = {
        call: call,
        register: register,
    };
    console.log('bridgeClientImpl init');
}

async function call(handlerName, args, callback) {
    if (!initialized) {
        initWeb();
    }

    let reqId = 0;
    if (callback && typeof callback === 'function') {
        reqId = ++requestId;
        callbackMap.set(reqId, callback);
    }

    let appUrl = location.href;
    let obj = { type: 'wf-op-request', requestId: reqId, windowId, appUrl, handlerName, args };
    console.log('wf-op-request', obj);

    window.parent.postMessage(obj, parentOrigin && parentOrigin !== 'null' ? parentOrigin : '*');
}

function handleOpResponse(requestId, args) {
    console.log('handle op response', requestId, args);
    let cb = callbackMap.get(requestId);
    if (cb) {
        cb(args);
        callbackMap.delete(requestId);
    }
}

function handleOpEvent(handlerName, args) {
    eventListeners[handlerName] && eventListeners[handlerName](args);
}

function register(handlerName, callback) {
    eventListeners[handlerName] = callback;
}
