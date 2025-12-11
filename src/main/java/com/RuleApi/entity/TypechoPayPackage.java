package com.RuleApi.entity;

import java.io.Serializable;
import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * TypechoPayPackage
 * @author pay 2025-10-18
 */
@Data
public class TypechoPayPackage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id  
     */
    private Integer id;

    /**
     * name  套餐名称
     */
    private String name;

    /**
     * intro  套餐描述
     */
    private String intro;

    /**
     * price  套餐价格
     */
    private Integer price;

    /**
     * gold  金币到账数量
     */
    private Integer gold;

    /**
     * integral  积分套餐数量
     */
    private Integer integral;

    /**
     * appleProductId  Apple专用，ProductId
     */
    private String appleProductId;
}