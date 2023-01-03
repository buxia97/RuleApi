package com.RuleApi.dao;

import com.RuleApi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoFanDao
 * @author buxia97
 * @date 2023/01/03
 */
@Mapper
public interface TypechoFanDao {

    /**
     * [新增]
     **/
    int insert(TypechoFan typechoFan);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoFan> list);

    /**
     * [更新]
     **/
    int update(TypechoFan typechoFan);

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
    TypechoFan selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoFan> selectList (TypechoFan typechoFan);

    /**
     * [分页条件查询]
     **/
    List<TypechoFan> selectPage (@Param("typechoFan") TypechoFan typechoFan, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoFan typechoFan);
}
