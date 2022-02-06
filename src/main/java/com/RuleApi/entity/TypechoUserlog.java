package com.RuleApi.entity;

import java.io.Serializable;
import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * TypechoUserlog
 * @author buxia97 2022-01-06
 */
@Data
public class TypechoUserlog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id  
     */
    private Integer id;

    /**
     * uid  用户id
     */
    private Integer uid;

    /**
     * cid  
     */
    private Integer cid;

    /**
     * type  类型
     */
    private String type;

    /**
     * num  数值，用于后期扩展
     */
    private Integer num;

    /**
     * created  时间
     */
    private Integer created;

    /**
     * created  指向用户
     */
    private Integer toid;
}