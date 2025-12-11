package com.RuleApi.entity;

import java.io.Serializable;
import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * TypechoFiles
 * @author files 2025-09-13
 */
@Data
public class TypechoFiles implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id  
     */
    private Integer id;

    /**
     * name  文件名称
     */
    private String name;

    /**
     * md5  md5值
     */
    private String md5;

    /**
     * links  文件链接
     */
    private String links;

    /**
     * source  文件源
     */
    private String source;

    /**
     * created  创建时间
     */
    private Integer created;

    /**
     * uid  上传用户
     */
    private Integer uid;

    /**
     * type  文件类型
     */
    private String type;

    /**
     * suffix  文件后缀
     */
    private String suffix;

    /**
     * size  文件大小
     */
    private Long size;
}