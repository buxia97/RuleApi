package com.RuleApi.service;

import java.util.List;
import com.RuleApi.entity.*;
import com.RuleApi.common.PageList;

/**
 * 业务层
 * TypechoAllconfigService
 * @author config
 * @date 2024/12/16
 */
public interface AllconfigService {

    /**
     * [新增]
     **/
    int insert(TypechoAllconfig typechoAllconfig);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoAllconfig> list);

    /**
     * [更新]
     **/
    int update(TypechoAllconfig typechoAllconfig);

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
    TypechoAllconfig selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoAllconfig> selectList (TypechoAllconfig typechoAllconfig);

    /**
     * [分页条件查询]
     **/
    PageList<TypechoAllconfig> selectPage (TypechoAllconfig typechoAllconfig, Integer page, Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoAllconfig typechoAllconfig);
}
