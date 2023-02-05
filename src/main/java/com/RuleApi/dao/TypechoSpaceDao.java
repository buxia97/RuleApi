package com.RuleApi.dao;

import com.RuleApi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoSpaceDao
 * @author buxia97
 * @date 2023/02/05
 */
@Mapper
public interface TypechoSpaceDao {

    /**
     * [新增]
     **/
    int insert(TypechoSpace typechoSpace);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoSpace> list);

    /**
     * [更新]
     **/
    int update(TypechoSpace typechoSpace);

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
    TypechoSpace selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoSpace> selectList (TypechoSpace typechoSpace);

    /**
     * [分页条件查询]
     **/
    List<TypechoSpace> selectPage (@Param("typechoSpace") TypechoSpace typechoSpace, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoSpace typechoSpace);
}
