package com.RuleApi.dao;

import com.RuleApi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoApiconfigDao
 * @author apiconfig
 * @date 2022/04/28
 */
@Mapper
public interface TypechoApiconfigDao {

    /**
     * [新增]
     **/
    int insert(TypechoApiconfig typechoApiconfig);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoApiconfig> list);

    /**
     * [更新]
     **/
    int update(TypechoApiconfig typechoApiconfig);

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
    TypechoApiconfig selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoApiconfig> selectList (TypechoApiconfig typechoApiconfig);

    /**
     * [分页条件查询]
     **/
    List<TypechoApiconfig> selectPage (@Param("typechoApiconfig") TypechoApiconfig typechoApiconfig, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoApiconfig typechoApiconfig);
}
