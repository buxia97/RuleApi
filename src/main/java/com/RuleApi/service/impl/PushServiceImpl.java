package com.RuleApi.service.impl;

import com.RuleApi.entity.TypechoFields;
import com.RuleApi.service.PushService;
import com.getui.push.v2.sdk.ApiHelper;
import com.getui.push.v2.sdk.GtApiConfiguration;
import com.getui.push.v2.sdk.api.PushApi;
import com.getui.push.v2.sdk.common.ApiResult;
import com.getui.push.v2.sdk.dto.req.Audience;
import com.getui.push.v2.sdk.dto.req.Settings;
import com.getui.push.v2.sdk.dto.req.Strategy;
import com.getui.push.v2.sdk.dto.req.message.PushChannel;
import com.getui.push.v2.sdk.dto.req.message.PushDTO;
import com.getui.push.v2.sdk.dto.req.message.PushMessage;
import com.getui.push.v2.sdk.dto.req.message.android.AndroidDTO;
import com.getui.push.v2.sdk.dto.req.message.android.GTNotification;
import com.getui.push.v2.sdk.dto.req.message.android.ThirdNotification;
import com.getui.push.v2.sdk.dto.req.message.android.Ups;
import com.getui.push.v2.sdk.dto.req.message.ios.Alert;
import com.getui.push.v2.sdk.dto.req.message.ios.Aps;
import com.getui.push.v2.sdk.dto.req.message.ios.IosDTO;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PushServiceImpl implements PushService {
    /**
     * 发送单Cid消息
     */
    @Override
    public  void sendPushMsg(String cid, String title, String content, String ClickType, String pushText){
        GtApiConfiguration apiConfiguration = new GtApiConfiguration();
        //填写应用配置，参数在“Uni Push”下的“应用配置”页面中获取
        apiConfiguration.setAppId("");
        apiConfiguration.setAppKey("");
        apiConfiguration.setMasterSecret("");
        apiConfiguration.setDomain("https://restapi.getui.com/v2/");
        // 实例化ApiHelper对象，用于创建接口对象
        ApiHelper apiHelper = ApiHelper.build(apiConfiguration);
        // 创建对象，建议复用。目前有PushApi、StatisticApi、UserApi
        PushApi pushApi = apiHelper.creatApi(PushApi.class);
        //根据cid进行单推
        PushDTO<Audience> pushDTO = new PushDTO<Audience>();
        // 设置推送参数，requestid需要每次变化唯一
        pushDTO.setRequestId(System.currentTimeMillis() + "");
//        默认所有通道的策略选择1-4
//        1: 表示该消息在用户在线时推送个推通道，用户离线时推送厂商通道;
//        2: 表示该消息只通过厂商通道策略下发，不考虑用户是否在线;
//        3: 表示该消息只通过个推通道下发，不考虑用户是否在线；
//        4: 表示该消息优先从厂商通道下发，若消息内容在厂商通道代发失败后会从个推通道下发。
        Strategy strategy = new Strategy();
        strategy.setDef(4);
//        strategy.setSt(1);
        Settings settings = new Settings();
        settings.setStrategy(strategy);
        pushDTO.setSettings(settings);
        //消息有效期，走厂商消息必须设置该值
        settings.setTtl(3600000);
        /**** 设置个推通道参数 *****/
        PushMessage pushMessage = new PushMessage();
        pushDTO.setPushMessage(pushMessage);
        GTNotification notification = new GTNotification();
        pushMessage.setNotification(notification);
        notification.setTitle(title);
        notification.setBody(content);
//        intent：打开应用内特定页面，
//        url：打开网页地址，
//        payload：自定义消息内容启动应用，
//        payload_custom：自定义消息内容不启动应用，
//        startapp：打开应用首页，
//        none：纯通知，无后续动作
//        notification.setClickType("url");
//        notification.setUrl("https://www.getui.com");
        notification.setClickType(ClickType);
        if(ClickType.equals("url")){
            notification.setUrl(pushText);
        }
        if(ClickType.equals("payload")){
            notification.setPayload(pushText);
        }
        //在线走个推通道时推送的消息体
        //此格式的透传消息由 unipush 做了特殊处理，会自动展示通知栏。开发者也可自定义其它格式，在客户端自己处理。
        //pushMessage.setTransmission(" {title:\"" + title + "\",content:\"" + content + "\",payload:\"test\"}");
        // 设置接收人信息
        Audience audience = new Audience();
        pushDTO.setAudience(audience);
        audience.addCid(cid);
        //设置离线推送时的消息体
        PushChannel pushChannel = new PushChannel();
        //安卓离线厂商通道推送的消息体
        AndroidDTO androidDTO = new AndroidDTO();
        Ups ups = new Ups();
        ThirdNotification thirdNotification = new ThirdNotification();
        ups.setNotification(thirdNotification);
        thirdNotification.setTitle(title);
        thirdNotification.setBody(content);
//        thirdNotification.setClickType("intent");
//        //注意：intent参数必须按下方文档（特殊参数说明）要求的固定格式传值，intent错误会导致客户端无法收到消息
//        thirdNotification.setIntent("请填写固定格式的intent");
        thirdNotification.setClickType(ClickType);
        if(ClickType.equals("url")){
            thirdNotification.setUrl(pushText);
        }
        if(ClickType.equals("payload")){
            thirdNotification.setPayload(pushText);
        }

        androidDTO.setUps(ups);
        pushChannel.setAndroid(androidDTO);

        //ios离线apn通道推送的消息体
        Alert alert = new Alert();
        alert.setTitle(title);
        alert.setBody(content);
        Aps aps = new Aps();
        aps.setContentAvailable(0);
        aps.setSound("default");
        aps.setAlert(alert);
        IosDTO iosDTO = new IosDTO();
        iosDTO.setAps(aps);
        iosDTO.setType("notify");
        pushChannel.setIos(iosDTO);

        pushDTO.setPushChannel(pushChannel);

        // 进行cid单推
        ApiResult<Map<String, Map<String, String>>> apiResult = pushApi.pushToSingleByCid(pushDTO);
        if (apiResult.isSuccess()) {
            // success
            System.out.println(apiResult.getData());
        } else {
            // failed
            System.out.println("code:" + apiResult.getCode() + ", msg: " + apiResult.getMsg());
        }
    }
}
