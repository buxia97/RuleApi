package com.RuleApi.service;

import java.util.List;
import com.RuleApi.entity.*;
import com.RuleApi.common.PageList;

/**
 * 业务层
 * TypechoPaylogService
 * @author buxia97
 * @date 2022/02/07
 */
public interface PaylogService {

    /**
     * [新增]
     **/
    int insert(TypechoPaylog typechoPaylog);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoPaylog> list);

    /**
     * [更新]
     **/
    int update(TypechoPaylog typechoPaylog);

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
    TypechoPaylog selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoPaylog> selectList (TypechoPaylog typechoPaylog);

    /**
     * [分页条件查询]
     **/
    PageList<TypechoPaylog> selectPage (TypechoPaylog typechoPaylog, Integer page, Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoPaylog typechoPaylog);
}
