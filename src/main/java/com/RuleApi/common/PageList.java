package com.RuleApi.common;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * PageList分页处理类
 * PageList.java
 * @author buxia97
 * @date 2021/11/29
 */
@Data
public class PageList<T extends Serializable> {

    /**
     * 总记录数
     */
    private int totalCount;

    /**
     * 总页数
     */
    private int totalPageCount;

    /**
     * 开始查询的页数
     */
    private int startPageNo;
    /**
     * 搜索Key
     */
    private String searchKey;

    /**
     * 排序order
     */
    private String order;

    /**
     * 查询的偏移量【每页查询的最大条数】
     */
    private int pageSize;

    /**
     * 对象集合
     */
    private List<T> list = new ArrayList<>();

    /**
     * msg
     */
    private  String msg;
}