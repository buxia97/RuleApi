package com.RuleApi.dao;

import com.RuleApi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoRelationshipsDao
 * @author buxia97
 * @date 2021/11/29
 */
@Mapper
public interface TypechoRelationshipsDao {

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
    int batchDelete(List<Object> list);

    /**
     * [主键查询]
     **/
    List<TypechoRelationships> selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoRelationships> selectList (TypechoRelationships typechoRelationships);

    /**
     * [分页条件查询]
     **/
    List<TypechoRelationships> selectPage (@Param("typechoRelationships") TypechoRelationships typechoRelationships, @Param("page") Integer page, @Param("pageSize") Integer pageSize, @Param("order") String order);

    /**
     * [总量查询]
     **/
    int total(TypechoRelationships typechoRelationships);
}
