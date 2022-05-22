package cn.wildfirechat.app.shiro;


import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.pojos.OutputApplicationUserInfo;
import cn.wildfirechat.sdk.ChannelServiceApi;
import cn.wildfirechat.sdk.UserAdmin;
import cn.wildfirechat.sdk.model.IMResult;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;

@Service
public class AuthCodeRealm extends AuthorizingRealm {

    @Autowired
    private AuthCodeMatcher authCodeMatcher;

    @Autowired
    private ChannelServiceApi channelServiceApi;

    @PostConstruct
    private void initMatcher() {
        setCredentialsMatcher(authCodeMatcher);
    }

    @Override
    public Class getAuthenticationTokenClass() {
        return AuthCodeToken.class;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        Set<String> stringSet = new HashSet<>();
        stringSet.add("user:view");
        info.setStringPermissions(stringSet);

        return info;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        if (authenticationToken instanceof AuthCodeToken) {
            AuthCodeToken authCodeToken = (AuthCodeToken) authenticationToken;
            try {
                IMResult<OutputApplicationUserInfo> applicationUserInfoIMResult = channelServiceApi.applicationGetUserInfo(authCodeToken.authCode);
                if (applicationUserInfoIMResult != null && applicationUserInfoIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                    SimpleAuthenticationInfo simpleAuthenticationInfo = new SimpleAuthenticationInfo(applicationUserInfoIMResult.getResult().getUserId(), null, getName());
                    Subject subject = SecurityUtils.getSubject();
                    subject.getSession().setAttribute("displayName", applicationUserInfoIMResult.getResult().getDisplayName());
                    subject.getSession().setAttribute("portraitUrl", applicationUserInfoIMResult.getResult().getPortraitUrl());
                    return simpleAuthenticationInfo;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}