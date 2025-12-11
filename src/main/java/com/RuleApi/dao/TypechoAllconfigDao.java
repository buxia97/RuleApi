package com.RuleApi.dao;

import com.RuleApi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoAllconfigDao
 * @author config
 * @date 2024/12/16
 */
@Mapper
public interface TypechoAllconfigDao {

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
    int batchDelete(List<Object> list);

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
    List<TypechoAllconfig> selectPage (@Param("typechoAllconfig") TypechoAllconfig typechoAllconfig, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoAllconfig typechoAllconfig);
}
