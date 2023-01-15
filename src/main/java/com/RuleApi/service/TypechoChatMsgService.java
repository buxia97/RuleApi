package com.RuleApi.service;

import java.util.Map;
import java.util.List;
import com.RuleApi.entity.*;
import com.RuleApi.common.PageList;

/**
 * 业务层
 * TypechoChatMsgService
 * @author buxia97
 * @date 2023/01/11
 */
public interface TypechoChatMsgService {

    /**
     * [新增]
     **/
    int insert(TypechoChatMsg typechoChatMsg);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoChatMsg> list);

    /**
     * [更新]
     **/
    int update(TypechoChatMsg typechoChatMsg);

    /**
     * [删除]
     **/
    int delete(Object key);
    int deleteMsg(Object key);

    /**
     * [批量删除]
     **/
    int batchDelete(List<Object> keys);

    /**
     * [主键查询]
     **/
    TypechoChatMsg selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoChatMsg> selectList (TypechoChatMsg typechoChatMsg);

    /**
     * [分页条件查询]
     **/
    PageList<TypechoChatMsg> selectPage (TypechoChatMsg typechoChatMsg, Integer page, Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoChatMsg typechoChatMsg);
}
