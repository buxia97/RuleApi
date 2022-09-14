package com.RuleApi.entity;

import java.io.Serializable;
import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * TypechoApiconfig
 * @author apiconfig 2022-04-28
 */
@Data
public class TypechoApiconfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id  
     */
    private Integer id;

    /**
     * webinfoTitle  网站名称
     */
    private String webinfoTitle;

    /**
     * webinfoUrl  网站URL
     */
    private String webinfoUrl;

    /**
     * webinfoUploadUrl  本地图片访问路径
     */
    private String webinfoUploadUrl;

    /**
     * webinfoAvatar  头像源
     */
    private String webinfoAvatar;

    /**
     * pexelsKey  图库key
     */
    private String pexelsKey;

    /**
     * scale  一元能买多少积分
     */
    private Integer scale;

    /**
     * clock  签到最多多少积分
     */
    private Integer clock;

    /**
     * vipPrice  VIP一天价格
     */
    private Integer vipPrice;

    /**
     * vipDay  多少天VIP等于永久
     */
    private Integer vipDay;

    /**
     * vipDiscount  VIP折扣
     */
    private String vipDiscount;

    /**
     * isEmail  是否开启邮箱注册（关闭后不再验证邮箱）
     */
    private Integer isEmail;

    /**
     * isInvite  注册是否验证邀请码（默认关闭）
     */
    private Integer isInvite;

    /**
     * cosAccessKey  
     */
    private String cosAccessKey;

    /**
     * cosSecretKey  
     */
    private String cosSecretKey;

    /**
     * cosBucket  
     */
    private String cosBucket;

    /**
     * cosBucketName  
     */
    private String cosBucketName;

    /**
     * cosPath  
     */
    private String cosPath;

    /**
     * cosPrefix  
     */
    private String cosPrefix;

    /**
     * aliyunEndpoint  
     */
    private String aliyunEndpoint;

    /**
     * aliyunAccessKeyId  
     */
    private String aliyunAccessKeyId;

    /**
     * aliyunAccessKeySecret  
     */
    private String aliyunAccessKeySecret;

    /**
     * aliyunAucketName  
     */
    private String aliyunAucketName;

    /**
     * aliyunUrlPrefix  
     */
    private String aliyunUrlPrefix;

    /**
     * aliyunFilePrefix  
     */
    private String aliyunFilePrefix;

    /**
     * ftpHost  
     */
    private String ftpHost;

    /**
     * ftpPort  
     */
    private Integer ftpPort;

    /**
     * ftpUsername  
     */
    private String ftpUsername;

    /**
     * ftpPassword  
     */
    private String ftpPassword;

    /**
     * ftpBasePath  
     */
    private String ftpBasePath;

    /**
     * alipayAppId  
     */
    private String alipayAppId;

    /**
     * alipayPrivateKey  
     */
    private String alipayPrivateKey;

    /**
     * alipayPublicKey  
     */
    private String alipayPublicKey;

    /**
     * alipayNotifyUrl  
     */
    private String alipayNotifyUrl;

    /**
     * appletsAppid  
     */
    private String appletsAppid;

    /**
     * appletsSecret  
     */
    private String appletsSecret;

    /**
     * qqAppletsAppid
     */
    private String qqAppletsAppid;

    /**
     * qqAppletsSecret
     */
    private String qqAppletsSecret;

    /**
     * wxpayAppId  
     */
    private String wxpayAppId;

    /**
     * wxpayMchId  
     */
    private String wxpayMchId;

    /**
     * wxpayKey  
     */
    private String wxpayKey;

    /**
     * wxpayNotifyUrl  
     */
    private String wxpayNotifyUrl;

    /**
     * auditlevel
     */
    private Integer auditlevel;

    /**
     * forbidden
     */
    private String forbidden;

    /**
     * fields
     */
    private String fields;

    /**
     * pushAdsPrice
     */
    private Integer pushAdsPrice;

    /**
     * pushAdsNum
     */
    private Integer pushAdsNum;

    /**
     * bannerAdsPrice
     */
    private Integer bannerAdsPrice;

    /**
     * bannerAdsNum
     */
    private Integer bannerAdsNum;

    /**
     * startAdsPrice
     */
    private Integer startAdsPrice;

    /**
     * startAdsNum
     */
    private Integer startAdsNum;

    /**
     * epayUrl
     */
    private String epayUrl;

    /**
     * epayPid
     */
    private Integer epayPid;

    /**
     * epayKey
     */
    private String epayKey;

    /**
     * epayNotifyUrl
     */
    private String epayNotifyUrl;

}