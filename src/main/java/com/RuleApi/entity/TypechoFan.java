package com.RuleApi.entity;

import java.io.Serializable;
import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * TypechoFan
 * @author buxia97 2023-01-03
 */
@Data
public class TypechoFan implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id  
     */
    private Integer id;

    /**
     * created  关注时间
     */
    private Integer created;

    /**
     * uid  关注人
     */
    private Integer uid;

    /**
     * touid  被关注人
     */
    private Integer touid;

    /**
     * type  帖子类型
     */
    private String type;
}