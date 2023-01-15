package com.RuleApi.service;

import java.util.Map;
import java.util.List;
import com.RuleApi.entity.*;
import com.RuleApi.common.PageList;

/**
 * 业务层
 * TypechoChatService
 * @author buxia97
 * @date 2023/01/10
 */
public interface TypechoChatService {

    /**
     * [新增]
     **/
    int insert(TypechoChat typechoChat);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoChat> list);

    /**
     * [更新]
     **/
    int update(TypechoChat typechoChat);

    /**
     * [删除]
     **/
    int delete(Object key);

    /**
     * [批量删除]
     **/
    int batchDelete(List<Object> keys);

    /**
     * [主键查询]
     **/
    TypechoChat selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoChat> selectList (TypechoChat typechoChat);

    /**
     * [分页条件查询]
     **/
    PageList<TypechoChat> selectPage (TypechoChat typechoChat, Integer page, Integer pageSize,String order);

    /**
     * [总量查询]
     **/
    int total(TypechoChat typechoChat);
}
