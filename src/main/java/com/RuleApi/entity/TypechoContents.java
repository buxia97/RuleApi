package com.RuleApi.entity;

import java.io.Serializable;
import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * TypechoContents
 * @author buxia97 2021-11-29
 */
@Data
public class TypechoContents implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * cid  
     */
    private Integer cid;

    /**
     * title  
     */
    private String title;

    /**
     * slug  
     */
    private String slug;

    /**
     * created  
     */
    private Integer created;

    /**
     * modified  
     */
    private Integer modified;

    /**
     * text  
     */
    private String text;

    /**
     * order  
     */
    private Integer orderKey;

    /**
     * authorId  
     */
    private Integer authorId;

    /**
     * template  
     */
    private String template;

    /**
     * type  
     */
    private String type;

    /**
     * status  
     */
    private String status;

    /**
     * password  
     */
    private String password;

    /**
     * commentsNum  
     */
    private Integer commentsNum;

    /**
     * allowComment  
     */
    private String allowComment;

    /**
     * allowPing  
     */
    private String allowPing;

    /**
     * allowFeed  
     */
    private String allowFeed;

    /**
     * parent  
     */
    private Integer parent;

    /**
     * views
     */
    private Integer views;

    /**
     * likes
     */
    private Integer likes;
    /**
     * isrecommend
     */
    private Integer isrecommend;

}