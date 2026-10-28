import {_handleNativeCall, bridge} from "@/jssdk/util";

export default class Biz {

    getAuthCode(appId, type, successCB, failCB) {
        bridge().call('getAuthCode', {
                appId,
                appType: type,
            }, _handleNativeCall(successCB, failCB)
        )
    }

    chooseContacts(options, successCB, failCB) {
        options = options ? options : {};
        options.max = options.max || 20;
        bridge().call('chooseContacts', options, _handleNativeCall(successCB, failCB));
    }
}
