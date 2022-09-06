package com.RuleApi.entity;

import java.io.Serializable;
import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * TypechoAds
 * @author ads 2022-09-06
 */
@Data
public class TypechoAds implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * aid  
     */
    private Integer aid;

    /**
     * name  广告名称
     */
    private String name;

    /**
     * type  广告类型（0推流，2横幅，3启动图）
     */
    private Integer type;

    /**
     * img  广告缩略图
     */
    private String img;

    /**
     * close  0代表永久，其它代表结束时间
     */
    private Integer close;

    /**
     * created  创建时间
     */
    private Integer created;

    /**
     * price  购买价格
     */
    private Integer price;

    /**
     * intro  广告简介
     */
    private String intro;

    /**
     * urltype  0为APP内部打开，1为跳出APP
     */
    private Integer urltype;

    /**
     * url  跳转Url
     */
    private String url;

    /**
     * 发布人
     */
    private Integer uid;
}