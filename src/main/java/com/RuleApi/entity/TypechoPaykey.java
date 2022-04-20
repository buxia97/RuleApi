package com.RuleApi.entity;

import java.io.Serializable;
import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * TypechoPaykey
 * @author paykey 2022-04-20
 */
@Data
public class TypechoPaykey implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id  
     */
    private Integer id;

    /**
     * value  密钥
     */
    private String value;

    /**
     * price  等值积分
     */
    private Integer price;

    /**
     * status  0未使用，1已使用
     */
    private Integer status;

    /**
     * created  创建时间
     */
    private Integer created;

    /**
     * uid  使用用户
     */
    private Integer uid;
}