package com.RuleApi.config;


import com.getui.push.v2.sdk.ApiHelper;
import com.getui.push.v2.sdk.GtApiConfiguration;
import com.getui.push.v2.sdk.api.PushApi;
import com.getui.push.v2.sdk.api.StatisticApi;
import com.getui.push.v2.sdk.api.UserApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @program: dashen-api
 * @description 个推配置
 * @author: fanzhen
 * @create: 2021-05-08 17:46
 **/
@Configuration
public class GeTuiConfig {
    @Value("${getui.baseUrl}")
    private String baseUrl;

    @Value("${getui.appId}")
    private String appId;

    @Value("${getui.appKey}")
    private String appKey;

    @Value("${getui.appSecret}")
    private String appSecret;

    @Value("${getui.masterSecret}")
    private String masterSecret;

    @Bean(name = "myPushApi")
    public PushApi pushApi() {
        GtApiConfiguration apiConfiguration = new GtApiConfiguration();
        //填写应用配置
        apiConfiguration.setAppId(appId);
        apiConfiguration.setAppKey(appKey);
        apiConfiguration.setMasterSecret(masterSecret);
        // 接口调用前缀，请查看文档: 接口调用规范 -> 接口前缀, 可不填写appId
        apiConfiguration.setDomain(baseUrl);
        // 实例化ApiHelper对象，用于创建接口对象
        ApiHelper apiHelper = ApiHelper.build(apiConfiguration);
        // 创建对象，建议复用。目前有PushApi、StatisticApi、UserApi
        PushApi pushApi = apiHelper.creatApi(PushApi.class);
        return pushApi;
    }

//    @Bean(name = "myStatisticApi")
//    public StatisticApi statisticApi() {
//        GtApiConfiguration apiConfiguration = new GtApiConfiguration();
//        //填写应用配置
//        apiConfiguration.setAppId(appId);
//        apiConfiguration.setAppKey(appKey);
//        apiConfiguration.setMasterSecret(masterSecret);
//        // 接口调用前缀，请查看文档: 接口调用规范 -> 接口前缀, 可不填写appId
//        apiConfiguration.setDomain(baseUrl);
//        // 实例化ApiHelper对象，用于创建接口对象
//        ApiHelper apiHelper = ApiHelper.build(apiConfiguration);
//        // 创建对象，建议复用。目前有PushApi、StatisticApi、UserApi
//        StatisticApi pushApi = apiHelper.creatApi(StatisticApi.class);
//        return pushApi;
//    }
//
//    @Bean(name = "myUserApi ")
//    public UserApi userApi() {
//        GtApiConfiguration apiConfiguration = new GtApiConfiguration();
//        //填写应用配置
//        apiConfiguration.setAppId(appId);
//        apiConfiguration.setAppKey(appKey);
//        apiConfiguration.setMasterSecret(masterSecret);
//        // 接口调用前缀，请查看文档: 接口调用规范 -> 接口前缀, 可不填写appId
//        apiConfiguration.setDomain(baseUrl);
//        // 实例化ApiHelper对象，用于创建接口对象
//        ApiHelper apiHelper = ApiHelper.build(apiConfiguration);
//        // 创建对象，建议复用。目前有PushApi、StatisticApi、UserApi
//        UserApi pushApi = apiHelper.creatApi(UserApi.class);
//        return pushApi;
//    }


}

