import {_handleNativeCall} from "@/jssdk/util";

const bridge = window.__wf_bridge_ ? window.__wf_bridge_ : require('dsbridge');

export default class Navigation {
    close(successCB, failCB) {
        bridge.call('close', _handleNativeCall(successCB, failCB));
    }
}