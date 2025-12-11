package com.RuleApi.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * TypechoUsers
 * @author buxia97 2021-11-29
 */
@Data
public class TypechoUsers implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * uid  
     */
    private Integer uid;

    /**
     * name  
     */
    private String name;

    /**
     * password  
     */
    private String password;

    /**
     * mail  
     */
    private String mail;

    /**
     * url  
     */
    private String url;

    /**
     * screenName  
     */
    private String screenName;

    /**
     * created  
     */
    private Integer created;

    /**
     * activated  
     */
    private Integer activated;

    /**
     * logged  
     */
    private Integer logged;

    /**
     * group  
     */
    private String groupKey;

    /**
     * authCode  
     */
    private String authCode;

    /**
     * introduce
     */
    private String introduce;

    /**
     * logged
     */
    private Integer assets;

    /**
     * address
     */
    private String address;

    /**
     * address
     */
    private String pay;

    /**
     * customize（自定义头衔）
     */
    private String customize;

    /**
     * vip（到期时间）
     */
    private Integer vip;

    /**
     * experience（经验值）
     */
    private Integer experience;

    /**
     * avatar（头像）
     */
    private String avatar;

    /**
     * 客户端id（用于发送状态栏消息）
     */
    private String clientId;

    /**
     * 封禁时间
     */
    private Integer bantime;

    /**
     * 最新发布时间
     */
    private Integer posttime;

    /**
     * 用户IP
     */
    private String ip;

    /**
     * 用户归属地
     */
    private String local;

    /**
     * 用户手机号
     */
    private String phone;

    /**
     * 用户主页背景图链接
     */
    private String userBg;

    /**
     * 用户邀请码
     */
    private String invitationCode;

    /**
     * 邀请用户
     */
    private Integer invitationUser;


    /**
     * 用户积分
     */
    private Integer points;

    /**
     * 佩戴荣誉
     */
    private Integer honor;

    /**
     * 用户性别(0未知，1男性，2女性)
     */
    private Integer gender;

    /**
     * 地区
     */
    private String region;

    /**
     * 出生日期
     */
    private Integer birthday;


}