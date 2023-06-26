package com.RuleApi.entity;

import java.io.Serializable;
import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * TypechoApp
 * @author vips 2023-06-09
 */
@Data
public class TypechoApp implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Integer id;

    /**
     * key  链接密钥
     */
    private String keyKey;

    /**
     * name  应用名称
     */
    private String name;

    /**
     * type  应用类型（web或App）
     */
    private String type;

    /**
     * logo  logo图标地址
     */
    private String logo;

    /**
     * keywords  web专属，SEO关键词
     */
    private String keywords;

    /**
     * description  应用简介
     */
    private String description;

    /**
     * announcement  弹窗公告（支持html）
     */
    private String announcement;

    /**
     * mail  邮箱地址（用于通知和显示）
     */
    private String mail;

    /**
     * website  网址（非Api地址）
     */
    private String website;

    /**
     * currencyName  货币名称
     */
    private String currencyName;

    /**
     * version  app专属，版本号
     */
    private String version;

    /**
     * versionCode  app专属，版本码
     */
    private Integer versionCode;

    /**
     * versionIntro  版本简介
     */
    private String versionIntro;

    /**
     * androidUrl  安卓下载地址
     */
    private String androidUrl;

    /**
     * iosUrl  ios下载地址
     */
    private String iosUrl;

    /**
     * adpid  广告联盟ID
     */
    private String adpid;

    /**
     * field1  预留字段1
     */
    private String field1;

    /**
     * field2  预留字段2
     */
    private String field2;
}