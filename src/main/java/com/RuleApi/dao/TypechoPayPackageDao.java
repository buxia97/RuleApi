package com.RuleApi.dao;

import com.RuleApi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoPayPackageDao
 * @author pay
 * @date 2025/10/18
 */
@Mapper
public interface TypechoPayPackageDao {

    /**
     * [新增]
     **/
    int insert(TypechoPayPackage typechoPayPackage);

    /**
     * [批量新增]
     **/
    int batchInsert(List<TypechoPayPackage> list);

    /**
     * [更新]
     **/
    int update(TypechoPayPackage typechoPayPackage);

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
    TypechoPayPackage selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<TypechoPayPackage> selectList (TypechoPayPackage typechoPayPackage);

    /**
     * [分页条件查询]
     **/
    List<TypechoPayPackage> selectPage (@Param("typechoPayPackage") TypechoPayPackage typechoPayPackage, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(TypechoPayPackage typechoPayPackage);
}
