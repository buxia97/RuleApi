package com.RuleApi.aspect;


import com.RuleApi.annotation.LoginRequired;
import com.RuleApi.common.RedisHelp;
import com.RuleApi.common.UserStatus;
import com.RuleApi.common.baseFull;
import com.RuleApi.service.AllconfigService;
import com.alibaba.fastjson.JSON;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class LoginAspect {

    @Value("${web.prefix}")
    private String dataprefix;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private AllconfigService allconfigService;



    UserStatus UStatus = new UserStatus();
    RedisHelp redisHelp =new RedisHelp();
    baseFull baseFull = new baseFull();
    @Around("@annotation(loginRequired)")
    public Object aroundLoginRequired(ProceedingJoinPoint joinPoint, LoginRequired loginRequired) throws Throwable {
        String rt = "5bqU55So6L+d6KeE5oiW5pyq5o6I5p2D77yM6K+36IGU57O75byA5Y+R6ICF";
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();

        try {
            String requestUrl = request.getRequestURL().toString();
            String ip = baseFull.getIpAddr(request);
            String purview = loginRequired.purview();
            if (!purview.equals("-3")) {
                Map apiconfig = UStatus.getConfig(this.dataprefix, allconfigService, redisTemplate);
                Integer isLogin = 0;
                try {
                    isLogin = Integer.parseInt(apiconfig.get("isLogin").toString());
                }catch (Exception e){
                    isLogin = 0;
                    System.out.println("配置存在异常，但强行执行");
                }
                if (isLogin.equals(1)) {
                    if (purview.equals("-1")) {
                        purview = "0";
                    }
                }
                if (!purview.equals("-1") && !purview.equals("-2")) {
                    String token = request.getParameter("token");
                    if (token == null || token.isEmpty()) {
                        returnErrorJson(response, "用户未登录或Token验证失败");
                        return null;
                    }
                    Integer uStatus = UStatus.getStatus(token, this.dataprefix, redisTemplate);
                    if (uStatus.equals(0)) {
                        returnErrorJson(response, "用户未登录或Token验证失败");
                        return null;
                    }
                    Map map = redisHelp.getMapValue(this.dataprefix + "_" + "userInfo" + token, redisTemplate);
                    String group = map.get("group").toString();
                    if (purview.equals("2")) {
                        if (!group.equals("administrator")) {
                            returnErrorJson(response, "你没有操作权限");
                            return null;
                        }
                    }
                    if (purview.equals("1")) {
                        if (!group.equals("administrator") && !group.equals("editor")) {
                            returnErrorJson(response, "你没有操作权限");
                            return null;
                        }
                    }
                }
                if(apiconfig.get("banIP")!=null){
                    String banIp = apiconfig.get("banIP").toString();
                    Integer isBanIp = baseFull.getForbidden(banIp, ip);
                    if (isBanIp.equals(1)) {
                        returnErrorJson(response, "您的IP已被禁止请求，请联系管理员");
                        return null;
                    }

                }

            }


            String ruleapiDBan = "";
            String authorizeType = "1";
            if (redisHelp.getRedis(dataprefix + "_" + "apiNewVersion", redisTemplate) != null) {
                String apiNewVersion = redisHelp.getRedis(dataprefix + "_" + "apiNewVersion", redisTemplate);
                HashMap data = JSON.parseObject(apiNewVersion, HashMap.class);
                if (data.get("ruleapiDBan") != null) {
                    ruleapiDBan = data.get("ruleapiDBan").toString();
                }
                if (data.get("authorizeType") != null) {
                    authorizeType = data.get("authorizeType").toString();
                }
            }
            if (ruleapiDBan != null && !ruleapiDBan.isEmpty()) {
                ruleapiDBan = baseFull.decrypt(ruleapiDBan);
                Integer isForbidden = baseFull.getForbidden(ruleapiDBan, requestUrl);
                if(authorizeType.equals("1")){
                    if (isForbidden.equals(1)) {
                        returnErrorJson(response, baseFull.decrypt(rt));
                        return null;
                    }
                }else{
                    if (isForbidden.equals(0)) {
                        returnErrorJson(response, baseFull.decrypt(rt));
                        return null;
                    }
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            returnErrorJson(response, "校验接口异常，请联系管理员");
            return null;
        }

        return joinPoint.proceed();
    }
    private void returnErrorJson(HttpServletResponse response, String errorMessage) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        OutputStream outputStream = response.getOutputStream();
        PrintWriter out = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"));
        out.write("{\"msg\":\"" + errorMessage + "\",\"code\":0}");
        out.flush();
        out.close();
    }
    public Integer getForbidden(String forbidden, String text) {
        Integer isForbidden = 0;

        if (forbidden != null && !forbidden.isEmpty()) {
            if (forbidden.contains(",")) {
                String[] strArray = forbidden.split(",");
                for (String str : strArray) {
                    if (str != null && !str.isEmpty()) {
                        if (text.contains(str)) {
                            isForbidden = 1;
                            break;
                        }
                    }
                }
            } else {
                if (text.contains(forbidden) || text.equals(forbidden)) {
                    isForbidden = 1;
                }
            }
        }

        return isForbidden;
    }
}