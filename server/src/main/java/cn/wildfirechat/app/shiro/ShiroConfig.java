package cn.wildfirechat.app.shiro;


import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.servlet.Cookie;
import org.apache.shiro.web.servlet.ShiroHttpSession;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class ShiroConfig {

    @Autowired
    DBSessionDao dbSessionDao;

    @Value("${wfc.all_client_support_ssl}")
    private boolean All_Client_Support_SSL;

    @Bean(name = "shiroFilter")
    public ShiroFilterFactoryBean shiroFilter(SecurityManager securityManager) {
        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        shiroFilterFactoryBean.setSecurityManager(securityManager);
        shiroFilterFactoryBean.setLoginUrl("/login");
        shiroFilterFactoryBean.setUnauthorizedUrl("/notRole");
        Map<String, String> filterChainDefinitionMap = new LinkedHashMap<>();

        // <!-- authc:所有url都必须认证通过才可以访问; anon:所有url都都可以匿名访问-->
        filterChainDefinitionMap.put("/index.html", "anon");
        filterChainDefinitionMap.put("/reportList.html", "anon");
        filterChainDefinitionMap.put("/myReports.html", "anon");
        filterChainDefinitionMap.put("/inbox.html", "anon");
        filterChainDefinitionMap.put("/newReport.html", "anon");
        filterChainDefinitionMap.put("/report.html", "anon");
        filterChainDefinitionMap.put("/favicon.ico", "anon");
        filterChainDefinitionMap.put("/js/*", "anon");
        filterChainDefinitionMap.put("/fonts/*", "anon");
        filterChainDefinitionMap.put("/img/*", "anon");
        filterChainDefinitionMap.put("/css/*", "anon");
        filterChainDefinitionMap.put("/", "anon");

        filterChainDefinitionMap.put("/api/login", "anon");
        filterChainDefinitionMap.put("/api/user/login", "anon");
        filterChainDefinitionMap.put("/api/user/config", "anon");
        filterChainDefinitionMap.put("/api/user/report", "login");
        filterChainDefinitionMap.put("/api/user/report_list", "login");

        //主要这行代码必须放在所有权限设置的最后，不然会导致所有 url 都被拦截 剩余的都需要认证
        filterChainDefinitionMap.put("/**", "login");
        shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);
        shiroFilterFactoryBean.getFilters().put("login", new JsonAuthLoginFilter());
        shiroFilterFactoryBean.getFilters().put("perms", new WfPermissionsAuthorizationFilter());
        return shiroFilterFactoryBean;

    }

    @Bean
    public SecurityManager securityManager() {
        DefaultWebSecurityManager defaultSecurityManager = new DefaultWebSecurityManager();
        defaultSecurityManager.setRealms(Arrays.asList(passwordRealm, authCodeRealm));
        ShiroSessionManager sessionManager = new ShiroSessionManager();
        // 30 分钟空闲超时；会话不再永久有效。
        sessionManager.setGlobalSessionTimeout(30 * 60 * 1000L);
        sessionManager.setSessionDAO(dbSessionDao);

        Cookie cookie = new SimpleCookie(ShiroHttpSession.DEFAULT_SESSION_ID_NAME);
        cookie.setHttpOnly(true);
        cookie.setSecure(All_Client_Support_SSL);
        cookie.setSameSite(All_Client_Support_SSL ? Cookie.SameSiteOptions.NONE : Cookie.SameSiteOptions.LAX);
        // 会话级 Cookie，关闭浏览器即失效；服务端仍有 30 分钟空闲超时。
        cookie.setMaxAge(-1);
        sessionManager.setSessionIdCookie(cookie);
        sessionManager.setSessionIdCookieEnabled(true);
        sessionManager.setSessionIdUrlRewritingEnabled(false);

        // 本项目不使用 rememberMe，显式关闭，避免“记住我”绕过完整认证。
        defaultSecurityManager.setRememberMeManager(null);
        defaultSecurityManager.setSessionManager(sessionManager);
        SecurityUtils.setSecurityManager(defaultSecurityManager);
        return defaultSecurityManager;
    }

    @Autowired
    private PasswordRealm passwordRealm;


    @Autowired
    private AuthCodeRealm authCodeRealm;
}