package com.RuleApi.dao;

import com.RuleApi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoFilesDao
 * @author files
 * @date 2025/09/13
 */
@Mapper
public interface TypechoFilesDao {

    /**
     * [新增]
     **/
    int insert(TypechoFiles typechoFiles);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoFiles> list);

    /**
     * [更新]
     **/
    int update(TypechoFiles typechoFiles);

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
    TypechoFiles selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoFiles> selectList (TypechoFiles typechoFiles);

    /**
     * [分页条件查询]
     **/
    List<TypechoFiles> selectPage (@Param("typechoFiles") TypechoFiles typechoFiles, @Param("page") Integer page, @Param("pageSize") Integer pageSize, @Param("searchKey") String searchKey, @Param("order") String order);

    /**
     * [总量查询]
     **/
    int total(@Param("typechoFiles") TypechoFiles typechoFiles, @Param("searchKey") String searchKey);
}
