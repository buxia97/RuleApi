package com.RuleApi.entity;

import java.io.Serializable;
import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * TypechoShoptype
 * @author shoptype 2023-07-10
 */
@Data
public class TypechoShoptype implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id  
     */
    private Integer id;

    /**
     * parent  上级分类
     */
    private Integer parent;

    /**
     * name  分类名称
     */
    private String name;

    /**
     * pic  分类缩略图
     */
    private String pic;

    /**
     * intro  分类简介
     */
    private String intro;

    /**
     * orderKey  分类排序
     */
    private Integer orderKey;
}