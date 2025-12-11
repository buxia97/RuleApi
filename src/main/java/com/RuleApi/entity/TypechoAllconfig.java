package com.RuleApi.entity;

import java.io.Serializable;
import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * TypechoAllconfig
 * @author config 2024-12-16
 */
@Data
public class TypechoAllconfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id  
     */
    private Integer id;

    /**
     * name  字段name
     */
    private String name;

    /**
     * type  字段类型，number，string，text
     */
    private String type;

    /**
     * value  字段值
     */
    private String value;

    /**
     * field  字段缩略名
     */
    private String field;

    /**
     * public  是否公开
     */
    private Integer isPublic;

    /**
     * class  所属类目ID
     */
    private Integer classId;

    /**
     * modules  所属模块ID
     */
    private Integer modules;

    /**
     * intro  字段简介
     */
    private String intro;

    /**
     * grade  排序权重
     */
    private Integer grade;
}