package com.RuleApi.dao;

import com.RuleApi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoChatDao
 * @author buxia97
 * @date 2023/01/10
 */
@Mapper
public interface TypechoChatDao {

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
    int batchDelete(List<Object> list);

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
    List<TypechoChat> selectPage (@Param("typechoChat") TypechoChat typechoChat, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoChat typechoChat);
}
