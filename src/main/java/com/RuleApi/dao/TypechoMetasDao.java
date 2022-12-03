package com.RuleApi.dao;

import com.RuleApi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoMetasDao
 * @author buxia97
 * @date 2021/11/29
 */
@Mapper
public interface TypechoMetasDao {

    /**
     * [新增]
     **/
    int insert(TypechoMetas typechoMetas);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoMetas> list);

    /**
     * [更新]
     **/
    int update(TypechoMetas typechoMetas);

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
    TypechoMetas selectByKey(Object key);

    /**
     * [slug查询]
     **/
    TypechoMetas selectBySlug(Object slug);

    /**
     * [条件查询]
     **/
    List<TypechoMetas> selectList (TypechoMetas typechoMetas);

    /**
     * [分页条件查询]
     **/
    List<TypechoMetas> selectPage (@Param("typechoMetas") TypechoMetas typechoMetas, @Param("page") Integer page, @Param("pageSize") Integer pageSize, @Param("searchKey") String searchKey, @Param("order") String order);

    /**
     * [总量查询]
     **/
    int total(TypechoMetas typechoMetas);
}
