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
     * customize
     */
    private String customize;
}