package com.RuleApi.entity;

import java.io.Serializable;
import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * TypechoFields
 * @author buxia97 2021-11-29
 */
@Data
public class TypechoFields implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * cid  
     */
    private Integer cid;

    /**
     * name  
     */
    private String name;

    /**
     * type  
     */
    private String type;

    /**
     * str_value  
     */
    private String strValue;

    /**
     * int_value  
     */
    private Integer intValue;

    /**
     * float_value  
     */
    private Float floatValue;
}