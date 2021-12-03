package com.RuleApi.dao;

import com.RuleApi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoFieldsDao
 * @author buxia97
 * @date 2021/11/29
 */
@Mapper
public interface TypechoFieldsDao {

    /**
     * [新增]
     **/
    int insert(TypechoFields typechoFields);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoFields> list);

    /**
     * [更新]
     **/
    int update(TypechoFields typechoFields);

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
    List<TypechoFields> selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoFields> selectList (TypechoFields typechoFields);

    /**
     * [分页条件查询]
     **/
    List<TypechoFields> selectPage (@Param("typechoFields") TypechoFields typechoFields, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoFields typechoFields);
}
