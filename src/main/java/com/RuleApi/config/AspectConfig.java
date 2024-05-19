package com.RuleApi.config;

import com.RuleApi.aspect.LoginAspect;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AspectConfig {


    public LoginAspect loginAspect() {
        return new LoginAspect();
    }
}