package com.RuleApi.entity;

import java.io.Serializable;
import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * TypechoChatMsg
 * @author buxia97 2023-01-11
 */
@Data
public class TypechoChatMsg implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id  
     */
    private Integer id;

    /**
     * uid  发送人
     */
    private Integer uid;

    /**
     * cid  聊天室
     */
    private Integer cid;

    /**
     * text  消息内容
     */
    private String text;

    /**
     * created  发送时间
     */
    private Integer created;

    /**
     * type  0文字消息，1图片消息，3视频消息
     */
    private Integer type;

    /**
     * url  为链接时的url
     */
    private String url;
}