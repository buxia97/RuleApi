package com.RuleApi.dao;

import com.RuleApi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoPaylogDao
 * @author buxia97
 * @date 2022/02/07
 */
@Mapper
public interface TypechoPaylogDao {

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
    int batchDelete(List<Object> list);

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
    List<TypechoPaylog> selectPage (@Param("typechoPaylog") TypechoPaylog typechoPaylog, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoPaylog typechoPaylog);
}
