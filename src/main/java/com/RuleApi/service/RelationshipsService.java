package com.RuleApi.service;

import java.util.List;
import com.RuleApi.entity.*;
import com.RuleApi.common.PageList;

/**
 * 业务层
 * TypechoRelationshipsService
 * @author buxia97
 * @date 2021/11/29
 */
public interface RelationshipsService {

    /**
     * [新增]
     **/
    int insert(TypechoRelationships typechoRelationships);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoRelationships> list);

    /**
     * [更新]
     **/
    int update(TypechoRelationships typechoRelationships);

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
    List<TypechoRelationships>  selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoRelationships> selectList (TypechoRelationships typechoRelationships);

    /**
     * [分页条件查询]
     **/
    PageList<TypechoRelationships> selectPage (TypechoRelationships typechoRelationships, Integer page, Integer pageSize,String order);

    /**
     * [总量查询]
     **/
    int total(TypechoRelationships typechoRelationships);
}
