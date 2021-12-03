package com.RuleApi.service;

import java.util.Map;
import java.util.List;
import com.RuleApi.entity.*;
import com.RuleApi.common.PageList;

/**
 * 业务层
 * TypechoMetasService
 * @author buxia97
 * @date 2021/11/29
 */
public interface TypechoMetasService {

    /**
     * [新增]
     **/
    int insert(TypechoMetas typechoMetas);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoMetas> list);

    /**
     * [更新]
     **/
    int update(TypechoMetas typechoMetas);

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
    TypechoMetas selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoMetas> selectList (TypechoMetas typechoMetas);

    /**
     * [分页条件查询]
     **/
    PageList<TypechoMetas> selectPage (TypechoMetas typechoMetas, Integer page, Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoMetas typechoMetas);
}
