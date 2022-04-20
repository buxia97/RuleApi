package com.RuleApi.dao;

import com.RuleApi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoPaykeyDao
 * @author paykey
 * @date 2022/04/20
 */
@Mapper
public interface TypechoPaykeyDao {

    /**
     * [新增]
     **/
    int insert(TypechoPaykey typechoPaykey);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoPaykey> list);

    /**
     * [更新]
     **/
    int update(TypechoPaykey typechoPaykey);

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
    TypechoPaykey selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoPaykey> selectList (TypechoPaykey typechoPaykey);

    /**
     * [分页条件查询]
     **/
    List<TypechoPaykey> selectPage (@Param("typechoPaykey") TypechoPaykey typechoPaykey, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoPaykey typechoPaykey);
}
