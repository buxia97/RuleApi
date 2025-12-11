package com.RuleApi.entity;

import java.io.Serializable;
import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * TypechoEmailtemplate
 * @author buxia97 2023-10-06
 */
@Data
public class TypechoEmailtemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id  
     */
    private Integer id;

    /**
     * verifyTemplate  验证码模板
     */
    private String verifyTemplate;

    /**
     * reviewTemplate  审核通知模板
     */
    private String reviewTemplate;

    /**
     * safetyTemplate  安全通知模板
     */
    private String safetyTemplate;

    /**
     * replyTemplate  评论&回复通知模板
     */
    private String replyTemplate;

    /**
     * orderTemplate  订单通知模板
     */
    private String orderTemplate;
}