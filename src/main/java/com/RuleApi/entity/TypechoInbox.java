package com.RuleApi.entity;

import java.io.Serializable;
import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * TypechoInbox
 * @author inbox 2022-12-29
 */
@Data
public class TypechoInbox implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id  
     */
    private Integer id;

    /**
     * type  消息类型
     */
    private String type;

    /**
     * uid  消息发送人，0是平台
     */
    private Integer uid;

    /**
     * text  消息内容（只有简略信息）
     */
    private String text;

    /**
     * touid  消息接收人uid
     */
    private Integer touid;

    /**
     * isread  是否已读，0已读，1未读
     */
    private Integer isread;

    /**
     * value  消息指向内容的id，根据类型跳转
     */
    private Integer value;

    /**
     * created  创建时间
     */
    private Integer created;

}