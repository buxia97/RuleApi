package com.RuleApi.entity;

import java.io.Serializable;
import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * TypechoSpace
 * @author buxia97 2023-02-05
 */
@Data
public class TypechoSpace implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id  
     */
    private Integer id;

    /**
     * uid  发布者
     */
    private Integer uid;

    /**
     * created  发布时间
     */
    private Integer created;

    /**
     * modified  修改时间
     */
    private Integer modified;

    /**
     * text  内容
     */
    private String text;

    /**
     * pic  图片，自己拆分
     */
    private String pic;

    /**
     * type  0普通动态，1转发和发布文章，2转发动态，3动态评论
     */
    private Integer type;

    /**
     * likes  喜欢动态的数量
     */
    private Integer likes;

    /**
     * toid  文章id，动态id等
     */
    private Integer toid;

    /**
     * status  动态状态，0审核，1发布，2锁定
     */
    private Integer status;

    /**
     * onlyMe  仅自己可见
     */
    private Integer onlyMe;
}