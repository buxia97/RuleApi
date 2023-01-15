package com.RuleApi.dao;

import com.RuleApi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoChatMsgDao
 * @author buxia97
 * @date 2023/01/11
 */
@Mapper
public interface TypechoChatMsgDao {

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
    int batchDelete(List<Object> list);

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
    List<TypechoChatMsg> selectPage (@Param("typechoChatMsg") TypechoChatMsg typechoChatMsg, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoChatMsg typechoChatMsg);
}
