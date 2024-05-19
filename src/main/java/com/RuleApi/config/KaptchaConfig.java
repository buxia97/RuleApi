package com.RuleApi.config;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class KaptchaConfig {
    @Bean
    public DefaultKaptcha getDefaultKaptcha(){
        com.google.code.kaptcha.impl.DefaultKaptcha defaultKaptcha = new com.google.code.kaptcha.impl.DefaultKaptcha();
        Properties properties = new Properties();
        // 图片边框
        properties.put("kaptcha.border", "no");
        // 字体颜色
        properties.put("kaptcha.textproducer.font.color", "black");
        // 图片宽
        properties.put("kaptcha.image.width", "160");
        // 图片高
        properties.put("kaptcha.image.height", "40");
        // 字体大小
        properties.put("kaptcha.textproducer.font.size", "30");
        // 验证码长度
        properties.put("kaptcha.textproducer.char.space", "5");
        // 字体
        properties.setProperty("kaptcha.textproducer.font.names", "宋体,楷体,微软雅黑");
        Config config = new Config(properties);
        defaultKaptcha.setConfig(config);
        return defaultKaptcha;
    }
}