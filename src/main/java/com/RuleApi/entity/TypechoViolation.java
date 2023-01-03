package com.RuleApi.entity;

import java.io.Serializable;
import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * TypechoViolation
 * @author buxia97 2023-01-03
 */
@Data
public class TypechoViolation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id  
     */
    private Integer id;

    /**
     * uid  违规者uid
     */
    private Integer uid;

    /**
     * type  违规类型（finance财务，content内容，comment评论，attack攻击）
     */
    private String type;

    /**
     * text  具体原因
     */
    private String text;

    /**
     * created  违规时间
     */
    private Integer created;

    /**
     * handler  处理人，0为系统自动，其它为真实用户
     */
    private Integer handler;
}