package com.RuleApi.dao;

import com.RuleApi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoUserapiDao
 * @author buxia97
 * @date 2022/01/10
 */
@Mapper
public interface TypechoUserapiDao {

    /**
     * [新增]
     **/
    int insert(TypechoUserapi typechoUserapi);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoUserapi> list);

    /**
     * [更新]
     **/
    int update(TypechoUserapi typechoUserapi);

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
    TypechoUserapi selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoUserapi> selectList (TypechoUserapi typechoUserapi);

    /**
     * [分页条件查询]
     **/
    List<TypechoUserapi> selectPage (@Param("typechoUserapi") TypechoUserapi typechoUserapi, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoUserapi typechoUserapi);
}
