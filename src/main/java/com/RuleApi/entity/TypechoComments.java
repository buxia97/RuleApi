package com.RuleApi.entity;

import java.io.Serializable;
import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * TypechoComments
 * @author buxia97 2021-11-29
 */
@Data
public class TypechoComments implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * coid  
     */
    private Integer coid;

    /**
     * cid  
     */
    private Integer cid;

    /**
     * created  
     */
    private Integer created;

    /**
     * author  
     */
    private String author;

    /**
     * authorId  
     */
    private Integer authorId;

    /**
     * ownerId  
     */
    private Integer ownerId;

    /**
     * mail  
     */
    private String mail;

    /**
     * url  
     */
    private String url;

    /**
     * ip  
     */
    private String ip;

    /**
     * agent  
     */
    private String agent;

    /**
     * text  
     */
    private String text;

    /**
     * type  
     */
    private String type;

    /**
     * status  
     */
    private String status;

    /**
     * parent  
     */
    private Integer parent;
}