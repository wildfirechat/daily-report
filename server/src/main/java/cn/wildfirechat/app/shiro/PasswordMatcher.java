package cn.wildfirechat.app.shiro;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class PasswordMatcher implements CredentialsMatcher {

    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
        if (token instanceof UsernamePasswordToken && info instanceof SimpleAuthenticationInfo) {
            UsernamePasswordToken passwordToken = (UsernamePasswordToken) token;
            String pwd = new String(passwordToken.getPassword());
            SimpleAuthenticationInfo authenticationInfo = (SimpleAuthenticationInfo) info;
            String salt = authenticationInfo.getCredentialsSalt() == null
                ? "" : new String(authenticationInfo.getCredentialsSalt().getBytes(), StandardCharsets.UTF_8);

            return PasswordHasher.matches(pwd, String.valueOf(authenticationInfo.getCredentials()), salt);
        }
        return false;
    }
}
